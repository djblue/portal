(ns portal.test-ui-runner
  (:require [clojure.test :as t]
            [portal.test-runner :as runner]
            [portal.ui.state-test]))

(defn -main []
  (runner/run
   #(t/run-tests 'portal.ui.state-test)))

(-main)
