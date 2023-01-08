(ns tasks.prepl
  (:require [clojure.core.server :as server]
            [portal.runtime.fs :as fs])
  #?(:clj
     (:import
      (java.net Socket))
     :cljr
     (:import
      (System.Net.Sockets TcpListener)
      (System.Threading Thread))))

(defn- get-runtime []
  #?(:bb :bb :clj :clj :cljr :cljr))

(defn- get-server-info [server]
  #?(:clj (let [server ^Socket server]
            {:host (-> server .getInetAddress .getCanonicalHostName)
             :port (.getLocalPort server)})
     :cljr (let [endpoint (.LocalEndpoint ^TcpListener server)]
             (while (zero? (.Port endpoint))
               (Thread/Sleep 100))
             {:host (.ToString (.Address endpoint)) :port (.Port endpoint)})
     :cljs server))

(defn- start-server [opts]
  (let [server (server/start-server opts)]
    #_{:clj-kondo/ignore #?(:cljs [:unresolved-symbol] :default [])}
    (future
      (let [info      (assoc (get-server-info server) :runtime (get-runtime))
            port-file (str "." (name (:runtime info)) "-repl")]
        (fs/rm-exit port-file)
        (fs/spit port-file (pr-str info))
        (println (str "=> " (:runtime info) " prepl listening on " (:host info) ":" (:port info)))))
    nil))

(defn- -pr-str [v]
  (binding [*print-meta* true] (pr-str v)))

(defn prepl []
  (start-server
   {:name          (name (get-runtime))
    :port          0
    :server-daemon false
    :args          [{:valf -pr-str}]
    :accept        `server/io-prepl}))
