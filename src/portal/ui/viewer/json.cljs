(ns portal.ui.viewer.json
  (:require [portal.ui.inspector :refer [inspector]]))

(defn- parse-json [json-string]
  (try (js->clj (js/JSON.parse json-string) :keywordize-keys true)
       (catch :default _e ::invalid)))

(defn json? [value] (string? value))

(defn inspect-json [settings json-string]
  [inspector settings (parse-json json-string)])
