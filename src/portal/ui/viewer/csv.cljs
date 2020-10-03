(ns portal.ui.viewer.csv
  (:require [portal.ui.inspector :refer [inspector]]
            ["csv-parse/lib/es5/sync" :as parse]))

(defn- parse-csv [csv-string]
  (try (js->clj (parse csv-string)) (catch :default _e ::invalid)))

(defn csv? [value] (string? value))

(defn inspect-csv [settings csv-string]
  [inspector settings (parse-csv csv-string)])

