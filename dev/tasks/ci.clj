(ns tasks.ci
  (:require [tasks.check :refer [check check*]]
            [tasks.test :refer [test test*]]))

(defn ci
  "Run all CI Checks."
  []
  (check) (test))

(defn -main [] (ci))

(comment
  (require '[tasks.parallel :refer [with-out-data]])
  (with-out-data (check*) (test*)))
