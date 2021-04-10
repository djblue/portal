(ns portal.test-runner
  (:require [cljs.test :refer [run-tests]]
            [portal.runtime.cson-test]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(defn -main []
  (run-tests 'portal.runtime.cson-test))

(-main)
