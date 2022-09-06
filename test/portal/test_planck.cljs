(ns portal.test-planck
  (:require [cljs.test :refer [run-tests]]
            [planck.core :refer [exit slurp]]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.json :as json]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (exit 1)))

(defn -main []
  (run-tests 'portal.runtime.cson-test)
  (prn)
  (bench/run (json/read (slurp "package-lock.json")) 50))
