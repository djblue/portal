(ns portal.test-planck
  (:require [cljs.test :refer [run-tests]]
            [clojure.pprint :as pp]
            [planck.core :refer [exit slurp]]
            [planck.environ :refer [env]]
            [portal.client.planck :as p]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.json :as json]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (exit 1)))

(def port (:portal-port env))

(defn submit [value] (p/submit {:port port} value))

(defn table [value]
  (if port
    (submit value)
    (pp/print-table
     (get-in (meta value) [:portal.viewer/table :columns])
     value)))

(defn -main []
  (run-tests 'portal.runtime.cson-test)
  (table (bench/run (json/read (slurp "package-lock.json")) 50)))
