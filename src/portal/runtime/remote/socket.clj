(ns portal.runtime.remote.socket
  "Fork of https://github.com/mfikes/tubular"
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:refer-clojure :exclude [load-file])
  (:import (java.net Socket)))

(defn create-socket
  [{:keys [^String hostname ^Integer port]
    :or   {hostname "localhost"}}]
  (Socket. hostname port))

(defn- write-line [socket message]
  (let [out (io/writer socket)]
    (.write out (str message "\n"))
    (.flush out)))

(defn- read-lines [^Socket socket] (line-seq (io/reader socket)))

(defn- load-file [options name]
  (let [socket (create-socket options)]
    (write-line socket (-> name io/resource slurp))
    (write-line socket ":repl/quit")
    (count (read-lines socket))
    nil))

(defn handler [socket message]
  (let [message (-> message json/parse-string pr-str)]
    (write-line
     socket
     (pr-str (list 'portal.runtime.remote.server/request message)))))

(defn responses [socket]
  (keep
   (fn [message]
     (let [message (str/replace-first message "user=> " "")]
       (when (str/starts-with? message "[")
         (-> message edn/read-string json/generate-string))))
   (read-lines socket)))

(defn quit [socket]
  (write-line socket ":repl/quit"))

(defn setup [options]
  (load-file options "portal/sync.cljc")
  (load-file options "portal/runtime/cson.cljc")
  (load-file options "portal/runtime.cljc")
  (load-file options "portal/runtime/jvm/client.clj")
  (load-file options "portal/runtime/remote/server.clj"))

(comment
  (def options {:hostname "localhost" :port 5555})
  (def socket (create-socket options))

  (responses socket))
