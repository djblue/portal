(ns portal.runtime.remote.socket
  "Fork of https://github.com/mfikes/tubular"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            ;[cheshire.core :as json]
            ;[clojure.edn :as edn]
            )
  (:refer-clojure :exclude [eval load load-file])
  (:import (java.net Socket)))

(defn- create-socket
  ^Socket [{:keys [options]}]
  (Socket.
   ^String  (get-in options [:runtime :hostname] "localhost")
   ^Integer (get-in options [:runtime :port])))

(defn- write-line [socket message]
  (let [out (io/writer socket)]
    (.write out (str message "\n"))
    (.flush out)))

(defn- read-lines [^Socket socket] (line-seq (io/reader socket)))

(defn- tap-out [string ^Socket socket]
  (write-line socket ":repl/quit")
  {:stdin string
   :stdout
   (->> (read-lines socket)
        (take-while #(not (str/ends-with? % ":repl/quit")))
        (str/join "\n"))})

(defn load [session string]
  (with-open [socket (create-socket session)]
    (write-line socket string)
    (tap-out string socket)))

(defn- load-file [session name]
  (load session (-> name io/resource slurp)))

(defn handler [{:keys [socket session-id]} message]
  (let [;message (-> message json/parse-string pr-str)
        request (pr-str
                 (list
                  'portal.runtime.remote.server/request
                  session-id
                  message))]
    (tap> [:request session-id message])
    (write-line socket request)))

(defn responses [{:keys [socket session-id]}]
  (keep
   (fn [message]
     (let [message (-> message
                       (str/replace-first "cljs.user=> " "")
                       (str/replace-first "user=> " ""))]
       (when (str/starts-with? message "[")
         (tap> [:response session-id message])
         (-> message ;edn/read-string json/generate-string
             ))))
   (read-lines socket)))

(defn close [{:keys [^Socket socket] :as session}]
  (load session
        (pr-str
         (list
          'portal.runtime.remote.server/close
          (:session-id session))))
  (future-cancel (:poller session))
  (.close socket))

(defn setup [session]
  (load-file session "portal/sync.cljc")
  (load-file session "portal/async.cljc")
  (load-file session "portal/runtime/macros.cljc")
  (load-file session "portal/runtime/cson.cljc")
  (load-file session "portal/runtime.cljc")

  (load-file session "portal/runtime/jvm/client.clj")
  (load-file session "portal/runtime/node/client.cljs")

  (load-file session "portal/runtime/remote/server.cljc"))

(defn open [session]
  (setup session)
  (let [socket (create-socket session)]
    (write-line
     socket
     (pr-str
      (list
       'portal.runtime.remote.server/open
       (:session-id session))))
    (-> session
        (assoc
         :poller
         (future
           (while true
             (write-line
              socket
              (pr-str
               (list
                'portal.runtime.remote.server/responses
                (:session-id session))))
             (Thread/sleep 300))))
        (assoc :socket socket))))

(comment
  (def options {:hostname "localhost" :port 5555})
  (def socket (create-socket options))

  (responses socket))
