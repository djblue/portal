(ns ^:no-doc portal.ui.viewer.csv
  (:require ["papaparse" :refer [parse]]
            [portal.ui.inspector :as ins]
            [portal.ui.parsers :as p]))

(defn parse-csv [csv-string]
  (try
    (with-meta
      (js->clj (.-data (parse csv-string)))
      {:portal.viewer/default :portal.viewer/table})
    (catch :default _e ::invalid)))

(defmethod p/parse-string :format/csv [_ s] (parse-csv s))

(defn csv? [value] (string? value))

(defn inspect-csv [csv-string]
  [ins/tabs
   {:portal.viewer/csv (parse-csv csv-string)
    "..."              csv-string}])

(def viewer
  {:predicate csv?
   :component #'inspect-csv
   :name :portal.viewer/csv
   :doc "Parse a string as a CSV and use the table viewer by default."})
