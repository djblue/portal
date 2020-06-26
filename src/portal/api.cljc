(ns portal.api
  (:require [portal.main :as m]
            [portal.runtime :as rt]))

(defn tap
  "Add portal as a tap> target."
  []
  (add-tap #'rt/update-value))

(defn open
  "Open a new inspector window."
  []
  (m/open-inspector))

(defn close
  "Close all current inspector windows."
  []
  (m/close-inspector))

(defn clear
  "Clear all values."
  []
  (rt/clear-values))

