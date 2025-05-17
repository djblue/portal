(ns tasks.cljr
  (:require [tasks.tools :refer [*opts* cljr]]))

(defn repl []
  (binding [*opts* {:inherit true}]
    (cljr "--eval" "((requiring-resolve 'tasks.prepl/prepl))" "--repl")))

(defn -main [] (repl))
