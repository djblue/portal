(ns tasks.dev
  (:require [clojure.core.server :as server]
            [clojure.java.io :as io]
            [tasks.build :refer [build]]
            [tasks.tools :refer [*opts* clj]]))

(defn- start-server [opts]
  (let [server    (server/start-server opts)
        port      (.getLocalPort server)
        host      (-> server .getInetAddress .getCanonicalHostName)
        port-file (io/file ".bb-repl")]
    (.deleteOnExit port-file)
    (spit port-file (pr-str {:host host :port port :runtime :bb}))
    (printf "=> Babashka prepl listening on %s:%s\n" host port)))

(defn pr-str* [v]
  (binding [*print-meta* true] (pr-str v)))

(defn prepl []
  (start-server
   {:name          "bb"
    :port          0
    :server-daemon false
    :args          [{:valf pr-str*}]
    :accept        clojure.core.server/io-prepl}))

(defn dev
  "Start dev server."
  []
  (binding [*opts* {:inherit true}]
    (build)
    (clj "-M:dev:cider:cljs:shadow"
         "-m" "shadow.cljs.devtools.cli"
         :watch #_:pwa :client :vs-code :vs-code-notebook #_:electron)))

(defn -main [] (prepl) (dev))
