(ns portal.test-runner
  (:require [clojure.pprint :as pp]
            [clojure.test :refer [run-tests]]
            [portal.jvm-test]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests 'portal.jvm-test
                   'portal.runtime.cson-test
                   'portal.runtime.fs-test)]
    (pp/print-table (bench/run (json/read (slurp "package-lock.json")) 50))
    (prn)
    (shutdown-agents)
    (System/exit (+ fail error))))
