(ns portal.api
  (:require [portal.main :as m]
            [portal.runtime :as rt]
            [portal.spec :as s]))

(defn tap
  "Add portal as a tap> target."
  []
  (add-tap #'rt/update-value)
  nil)

(defn open
  "Open a new inspector window."
  ([] (open nil))
  ([options]
   (s/assert-options options)
   (m/open-inspector options)
   nil))

(defn close
  "Close all current inspector windows."
  []
  (m/close-inspector)
  nil)

(defn clear
  "Clear all values."
  []
  (rt/clear-values)
  nil)

