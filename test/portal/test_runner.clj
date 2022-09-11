(ns portal.test-runner
  (:require [clojure.pprint :as pp]
            [clojure.test :as t]
            [portal.client.jvm :as p]
            [portal.jvm-test]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]))

(def port (System/getenv "PORTAL_PORT"))

(defn submit [value] (p/submit {:port port} value))

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
        (run-tests 'portal.jvm-test
                   'portal.runtime.cson-test
                   'portal.runtime.fs-test)]
    (table (bench/run (json/read (slurp "package-lock.json")) 50))
    (shutdown-agents)
    (System/exit (+ fail error))))
