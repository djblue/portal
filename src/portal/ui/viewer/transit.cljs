(ns portal.ui.viewer.transit
  (:require [cognitect.transit :as t]
            [portal.ui.inspector :as ins]))

(defn- parse-transit [transit-string]
  (try (t/read (t/reader :json) transit-string)
       (catch :default _e ::invalid)))

(defn transit? [value] (string? value))

(defn inspect-transit [transit-string]
  [ins/inspector (parse-transit transit-string)])

(def viewer
  {:predicate transit?
   :component inspect-transit
   :name :portal.viewer/transit})
