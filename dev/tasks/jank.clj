(ns tasks.jank
  (:require
   [portal.api :as p]
   [tasks.tools :refer [*opts* jank]]))

(defn -main
  "Start jank dev env / nrepl"
  []
  (let [server (p/start {})]
    (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (p/close))))
    (p/open {:launcher :auto})
    (binding [*opts* {:extra-env {"PORTAL_PORT" (:port server)}}]
      (jank :repl))))