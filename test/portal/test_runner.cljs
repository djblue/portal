(ns portal.test-runner
  (:require [cljs.test :refer [run-tests]]
            [clojure.pprint :as pp]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.fs :as fs]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(defn -main []
  (run-tests 'portal.runtime.cson-test
             'portal.runtime.fs-test)
  (pp/print-table (bench/run (json/read (fs/slurp "package-lock.json")) 100))
  (prn))

(-main)
