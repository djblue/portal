(ns tasks.ci
  (:require [tasks.check :refer [check]]
            [tasks.test :refer [test]]))

(defn ci
  "Run all CI Checks."
  []
  (check) (test))

(defn -main [] (ci))
