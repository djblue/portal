(ns ^:no-doc portal.ui.viewer.transit
  (:require [cognitect.transit :as t]
            [portal.ui.inspector :as ins]
            [portal.ui.parsers :as p]))

(defn- parse-transit [transit-string]
  (try (t/read (t/reader :json) transit-string)
       (catch :default e (ins/error->data e))))

(defmethod p/parse-string :format/transit [_ s] (parse-transit s))

(defn transit? [value] (string? value))

(defn inspect-transit [transit-string]
  [ins/tabs
   {:portal.viewer/transit (parse-transit transit-string)
    "..."                  transit-string}])

(def viewer
  {:predicate transit?
   :component #'inspect-transit
   :name :portal.viewer/transit
   :doc "Parse a string as transit. Will render error if parsing fails."})
