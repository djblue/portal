(ns portal.api
  (:require [portal.runtime :as rt]
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l])))

(defn submit
  "Tap target function."
  [value]
  (rt/update-value value)
  nil)

(defn ^{:deprecated "0.9"} tap
  "Add portal as a tap> target."
  []
  (add-tap #'submit)
  nil)

(defn start
  "Start the HTTP server with non-default options. Only use if you need
  control over the HTTP server."
  [options]
  (l/start options))

(defn open
  "Open a new inspector window. A previous instance can be passed as
  parameter to make sure it is open."
  ([] (open nil))
  ([portal-or-options]
   (if (:session-id portal-or-options)
     (l/open portal-or-options nil)
     (l/open nil portal-or-options))))

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
