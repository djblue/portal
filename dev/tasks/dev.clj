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
    (printf "=> Babashka prepl listening on %s:%s\n" host port)))

(defn prepl []
  (start-server
   {:name          "bb"
    :port          0
    :accept        clojure.core.server/io-prepl
    :server-daemon false}))

(defn dev
  "Start dev server."
  []
  (build)
  (clj "-M:dev:cider:cljs:shadow"
       "-m" "shadow.cljs.devtools.cli"
       :watch :pwa :client :vs-code :electron))

(defn -main [] (prepl) (dev))
