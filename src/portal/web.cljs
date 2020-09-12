(ns portal.web
  (:require [portal.runtime :as rt]
            [portal.runtime.client.web :as c]
            [portal.runtime.launcher.web :as l]
            [portal.spec :as s]))

(defonce do-init (l/init))

(def ^:export send! l/send!)

(defn ^:export tap
  "Add portal as a tap> target."
  []
  (add-tap #'rt/update-value)
  nil)

(defn ^:export open
  "Open a new inspector window."
  ([] (open nil))
  ([options]
   (s/assert-options options)
   (l/open options)
   (c/make-atom l/child-window)))

(defn ^:export close
  "Close all current inspector windows."
  []
  (l/close)
  nil)

(defn ^:export clear
  "Clear all values."
  []
  (rt/clear-values)
  nil)

