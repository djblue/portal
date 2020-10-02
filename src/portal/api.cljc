(ns portal.api
  (:require [portal.runtime :as rt]
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l])))

(defn tap
  "Add portal as a tap> target."
  []
  (add-tap #'rt/update-value)
  nil)

(defn open
  "Open a new inspector window."
  ([] (open nil))
  ([options]
   (l/open options)))

(defn close
  "Close all current inspector windows."
  []
  (l/close)
  nil)

(defn clear
  "Clear all values."
  []
  (rt/clear-values)
  nil)

