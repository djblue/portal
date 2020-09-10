(ns portal.test-runner
  (:require [clojure.test :refer [run-tests]]
            [portal.http-socket-server-test]
            [portal.jvm-test]))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests 'portal.http-socket-server-test
                   'portal.jvm-test)]
    (shutdown-agents)
    (System/exit (+ fail error))))

