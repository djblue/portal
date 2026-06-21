(ns tasks.jank
  (:require
   [tasks.tools :refer [jank]]))

(defn -main
  "Start jank dev env / nrepl"
  []
  (jank :repl))