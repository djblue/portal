(ns portal.ui.viewer.csv
  (:require ["papaparse" :refer [parse]]
            [portal.ui.inspector :refer [inspector]]))

(defn- parse-csv [csv-string]
  (try (js->clj (.-data (parse csv-string))) (catch :default _e ::invalid)))

(defn csv? [value] (string? value))

(defn inspect-csv [settings csv-string]
  [inspector settings (parse-csv csv-string)])

(def viewer
  {:predicate csv?
   :datafy parse-csv
   :component inspect-csv
   :name :portal.viewer/csv})
