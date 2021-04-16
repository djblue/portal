(ns portal.ui.viewer.csv
  (:require ["papaparse" :refer [parse]]
            [portal.ui.inspector :as ins]))

(defn parse-csv [csv-string]
  (try (js->clj (.-data (parse csv-string))) (catch :default _e ::invalid)))

(defn csv? [value] (string? value))

(defn inspect-csv [csv-string]
  [ins/inspector (parse-csv csv-string)])

(def viewer
  {:predicate csv?
   :component inspect-csv
   :name :portal.viewer/csv})
