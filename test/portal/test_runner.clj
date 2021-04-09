(ns portal.test-runner
  (:require [clojure.test :refer [run-tests]]
            [portal.jvm-test]
            [portal.runtime.cson-test]))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests 'portal.jvm-test
                   'portal.runtime.cson-test)]
    (shutdown-agents)
    (System/exit (+ fail error))))

