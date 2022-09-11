(ns portal.test-runner
  (:require [cljs.test :refer [run-tests]]
            [clojure.pprint :as pp]
            [portal.client.node :as p]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.fs :as fs]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(def port (.. js/process -env -PORTAL_PORT))

(defn submit [value] (p/submit {:port port} value))

(defn table [value]
  (if port
    (submit value)
    (pp/print-table
     (get-in (meta value) [:portal.viewer/table :columns])
     value)))

(defn -main []
  (run-tests 'portal.runtime.cson-test
             'portal.runtime.fs-test)
  (table (bench/run (json/read (fs/slurp "package-lock.json")) 100)))

(-main)
