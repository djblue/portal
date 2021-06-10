(ns portal.web
  (:require [portal.runtime :as rt]
            [portal.runtime.web.client :as c]
            [portal.runtime.web.launcher :as l]
            [portal.shortcuts :as shortcuts]))

(def ^:export send! l/send!)

(defn ^:export submit
  "Tap target function."
  [value]
  (rt/update-value value)
  nil)

(defn ^:export ^{:deprecated "0.9"} tap
  "Add portal as a tap> target."
  []
  (add-tap #'submit)
  nil)

(defn ^:export open
  "Open a new inspector window."
  ([] (open nil))
  ([options]
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
  (l/clear)
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
