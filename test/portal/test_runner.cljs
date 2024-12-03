(ns portal.test-runner
  (:require
   [clojure.pprint :as pp]
   [clojure.test :as t]
   [portal.async :as a]
   [portal.client.node :as p]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (when-not (cljs.test/successful? m)
    (.exit js/process 1)))

(def port (.. js/process -env -PORTAL_PORT))

(defn submit [value] (p/submit {:port port :encoding :cson} value))

(defn table [value]
  (if port
    (submit value)
    (pp/print-table
      (get-in (meta value) [:portal.viewer/table :columns])
      value)))

(defn run-tests [f]
  (if-not port
    (f)
    (a/let [report  (atom [])
            report' t/report]
      (set! t/report  #(swap! report conj %))
      (f)
      (set! t/report report')
      (submit @report)
      @report)))

(defn run [f]
  (a/let [report (run-tests f)
          errors (count (filter (comp #{:fail} :type) report))]
    (.exit js/process errors)))
