(ns portal.test-runner
  (:require [clojure.test :as t]
            [portal.async :as a]
            [portal.client :as p]
            [portal.console :as console]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(def ^:private requests (atom []))

(defn run-tests [f]
  (if-not (p/enabled?)
    (f)
    (a/let [report  (atom [])
            report' t/report
            summary (atom {})]
      (set! t/report (fn [message]
                       (when (= :fail (:type message))
                         (swap! summary update :fail (fnil inc 0)))
                       (swap! report conj
                              (assoc message
                                     :time    (console/now)
                                     :runtime (console/runtime)))
                       (when (= (:type message) :end-test-ns)
                         (swap! requests conj (p/submit @report))
                         (reset! report []))))
      (add-tap #'p/submit)
      (f)
      (remove-tap #'p/submit)
      (set! t/report report')
      @summary)))

(defn run [f]
  (a/let [{:keys [fail]} (run-tests f)]
    (.all js/Promise @requests)
    (.exit js/process fail) 0))
