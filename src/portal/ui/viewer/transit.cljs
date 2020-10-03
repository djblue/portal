(ns portal.ui.viewer.transit
  (:require [cognitect.transit :as t]
            [portal.ui.inspector :refer [inspector]]))

(defn- parse-transit [transit-string]
  (try (t/read (t/reader :json) transit-string)
       (catch :default _e ::invalid)))

(defn transit? [value] (string? value))

(defn inspect-transit [settings transit-string]
  [inspector settings (parse-transit transit-string)])

(def viewer
  {:predicate transit?
   :datafy parse-transit
   :component inspect-transit
   :name :portal.viewer/transit})
