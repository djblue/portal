(ns portal.test-runner
  (:require [clojure.test :as t]
            [portal.async :as a]
            [portal.client :as p]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(defn run-tests [f]
  (if-not (p/enabled?)
    (f)
    (a/let [report  (atom [])
            report' t/report]
      (set! t/report  #(swap! report conj %))
      (f)
      (set! t/report report')
      (p/submit @report)
      @report)))

(defn run [f]
  (a/let [report (run-tests f)
          errors (count (filter (comp #{:fail} :type) report))]
    (.exit js/process errors)))
