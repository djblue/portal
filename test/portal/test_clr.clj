(ns portal.test-clr
  (:require [clojure.pprint :as pp]
            [clojure.test :as t]
            [portal.client-test]
            [portal.client.clr :as p]
            [portal.runtime-test]
            [portal.runtime.api-test]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.edn-test]
            [portal.runtime.fs-test]
            [portal.runtime.json-buffer-test]
            [portal.runtime.npm-test]
            [portal.runtime.shell-test])
  (:import (System Environment)))

(def port (Environment/GetEnvironmentVariable "PORTAL_PORT"))

(defn submit [value] (p/submit {:port port :encoding :cson} value))

(defn table [value]
  (if port
    (submit value)
    (pp/print-table
     (get-in (meta value) [:portal.viewer/table :columns])
     value)))

(defn run-tests [& tests]
  (if-not port
    (apply t/run-tests tests)
    (let [report (atom [])
          counts
          (with-redefs [t/report #(swap! report conj %)]
            (apply t/run-tests tests))]
      (submit @report)
      counts)))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests
         'portal.client-test
         'portal.runtime-test
         'portal.runtime.api-test
         'portal.runtime.cson-test
         'portal.runtime.edn-test
         'portal.runtime.fs-test
         'portal.runtime.json-buffer-test
         'portal.runtime.npm-test
         'portal.runtime.shell-test)]
    (table (bench/run))
    (shutdown-agents)
    (Environment/Exit (+ fail error))))
