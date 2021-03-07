(ns portal.ui.viewer.json
  (:require [portal.ui.inspector :as ins]))

(defn- parse-json [json-string]
  (try (js->clj (js/JSON.parse json-string) :keywordize-keys true)
       (catch :default _e ::invalid)))

(defn json? [value] (string? value))

(defn inspect-json [json-string]
  [ins/inspector (parse-json json-string)])

(def viewer
  {:predicate json?
   :component inspect-json
   :name :portal.viewer/json})
