(ns portal.web
  (:require [portal.runtime :as rt]
            [portal.runtime.web.client :as c]
            [portal.runtime.web.launcher :as l]
            [portal.shortcuts :as shortcuts]
            [portal.spec :as s]))

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

(defonce init? (atom false))

(defn init []
  (when-not @init?
    (reset! init? true)
    (l/init)
    (shortcuts/add!
     ::init
     (fn [log]
       (when (shortcuts/match?
              {::shortcuts/osx     #{"meta" "shift" "o"}
               ::shortcuts/default #{"control" "shift" "o"}}
              log)
         (open))))))

(init)
