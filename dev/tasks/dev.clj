(ns tasks.dev
  (:require [clojure.core.server :as server]
            [clojure.java.io :as io]
            [tasks.build :refer [build]]
            [tasks.tools :refer [clj]]))

(defn- start-server [opts]
  (let [server    (server/start-server opts)
        port      (.getLocalPort server)
        host      (-> server .getInetAddress .getCanonicalHostName)
        port-file (io/file ".bb-repl")]
    (.deleteOnExit port-file)
    (spit port-file (pr-str {:host host :port port :runtime :bb}))
    (printf "=> Babashka socket repl listening on %s:%s\n" host port)))

(defn socket-repl []
  (start-server
   {:name          "bb"
    :port          0
    :accept        clojure.core.server/repl
    :server-daemon false}))

(defn dev
  "Start dev server."
  []
  (build)
  (clj "-M:dev:cider:cljs:shadow"
       :watch :pwa :client :vs-code :electron))

(defn -main [] (socket-repl) (dev))
