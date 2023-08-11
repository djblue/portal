(ns portal.test-clr
  (:require [clojure.pprint :as pp]
            [clojure.test :as t]
            [portal.client.clr :as p]
            [portal.runtime.api-test]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.edn-test]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]
            [portal.runtime.json-buffer-test]
            [portal.runtime.npm-test])
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
         'portal.runtime.api-test
         'portal.runtime.cson-test
         'portal.runtime.edn-test
         'portal.runtime.fs-test
         'portal.runtime.json-buffer-test
         'portal.runtime.npm-test)]
    (table (bench/run (json/read (slurp "package-lock.json" :encoding "utf8")) 50))
    (Environment/Exit (+ fail error))))
