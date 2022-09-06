(ns portal.test-runner
  (:require [clojure.test :refer [run-tests]]
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
    (prn)
    (bench/run (json/read (slurp "package-lock.json")) 50)
    (shutdown-agents)
    (System/exit (+ fail error))))
