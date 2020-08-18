(ns portal.test-runner
  (:require [clojure.test :refer [run-tests]]
            [portal.http-socket-server-test]))

(defn -main []
  (let [{:keys [fail error]} (run-tests 'portal.http-socket-server-test)]
    (shutdown-agents)
    (System/exit (+ fail error))))

