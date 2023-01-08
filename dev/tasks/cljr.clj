(ns tasks.cljr
  (:require [tasks.tools :refer [*opts* cljr]]))

(defn repl []
  (binding [*opts* {:inherit true
                    :extra-env {"CLOJURE_LOAD_PATH" "src:dev:test"}}]
    (cljr "--eval" "((requiring-resolve 'tasks.prepl/prepl))" "--repl")))

(defn -main [] (repl))
