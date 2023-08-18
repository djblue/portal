(ns ^:no-doc portal.ui.viewer.json
  (:require [portal.ui.inspector :as ins]))

(defn- parse-json [json-string]
  (try (js->clj (js/JSON.parse json-string) :keywordize-keys true)
       (catch :default e (ins/error->data e))))

(defn json? [value] (string? value))

(defn inspect-json [json-string]
  [ins/tabs
   {:portal.viewer/json (parse-json json-string)
    "..."               json-string}])

(def viewer
  {:predicate json?
   :component inspect-json
   :name :portal.viewer/json
   :doc "Parse a string as JSON. Will render error if parsing fails."})
