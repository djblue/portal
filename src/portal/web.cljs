(ns portal.web
  (:require [clojure.set :as set]
            [portal.runtime :as rt]
            [portal.runtime.web.client :as c]
            [portal.runtime.web.launcher :as l]
            [portal.shortcuts :as shortcuts]
            [portal.spec :as s]))

(def ^:export send! l/send!)

(defonce ^:private default-options (atom nil))

(defn set-defaults!
  "Set default options for `open`.
  Parameters passed directly to either will override defaults."
  {:added    "0.20.0"
   :see-also ["open"]}
  [options]
  (swap! default-options merge options))

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

(def ^:private long->short
  {:portal.colors/theme          :theme
   :portal.launcher/app          :app
   :portal.launcher/host         :host
   :portal.launcher/port         :port
   :portal.launcher/window-title :window-title})

(defn- rename [options]
  (set/rename-keys options long->short))

(defn ^:export open
  "Open a new inspector window."
  ([] (open nil))
  ([options]
   (s/assert-options options)
   (l/open (rename (merge @default-options options)))
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

(defn register!
  "Register a var with portal. For now, the var should be a 1 arity fn.

  Example: `(register! #'identity)`

  The function name and doc string will show up in the command palette."
  {:added "0.20.0"}
  [var]
  (rt/register! var)
  nil)

(defn eval-str
  "Evalute ClojureScript source given as a string in the UI runtime."
  {:added "0.19.0"}
  [source]
  (l/eval-str source)
  nil)

(defonce ^:private init? (atom false))

(defn- init []
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
