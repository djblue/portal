(ns tasks.dev
  (:require [clojure.core.server :as server]
            [clojure.java.io :as io]
            [org.httpkit.server :as http]
            [tasks.build :refer [build]]
            [tasks.tools :refer [*opts* clj]]))

(defrecord Edn [edn])

(defmethod print-method Edn [v ^java.io.Writer w]
  (.write w ^String (:edn v)))

(defn- proxy-tap> [request]
  (tap> (->Edn (slurp (:body request))))
  {:status 200})

(defn- start-server [opts]
  (let [server    (server/start-server opts)
        port      (.getLocalPort server)
        host      (-> server .getInetAddress .getCanonicalHostName)
        port-file (io/file ".bb-repl")]
    (.deleteOnExit port-file)
    (spit port-file (pr-str {:host host :port port :runtime :bb}))
    (printf "=> Babashka prepl listening on %s:%s\n" host port)))

(defn- io-prepl [& args]
  (binding [*opts* {:extra-env {"PORTAL_PORT" (-> args first ::port str)}}]
    (apply server/io-prepl args)))

(defmethod print-method (type #'var?) [v ^java.io.Writer w]
  (let [m (meta v)]
    (when *print-meta*
      (.write w "^")
      (.write w (pr-str m))
      (.write w " "))
    (.write w "#'")
    (.write w (str (symbol (str (:ns m))
                           (str (:name m)))))))

(defn- pr-str* [v]
  (binding [*print-meta* true] (pr-str v)))

(defn prepl []
  (let [server (http/run-server #'proxy-tap> {:legacy-return-value? false})
        port   (http/server-port server)]
    (start-server
     {:name          "bb"
      :port          0
      :server-daemon false
      :args          [{:valf pr-str* ::port port}]
      :accept        `io-prepl})))

(defn dev
  "Start dev server."
  []
  (binding [*opts* {:inherit true}]
    (build)
    (clj "-M:dev:cider:cljs:shadow"
         "-m" "shadow.cljs.devtools.cli"
         :watch #_:pwa :client :vs-code :vs-code-notebook #_:electron)))

(defn -main [] (prepl) (dev))
