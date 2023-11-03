(ns portal.web
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [portal.runtime :as rt]
            [portal.runtime.web.client :as c]
            [portal.runtime.web.launcher :as l]
            [portal.shortcuts :as shortcuts]
            [portal.spec :as s]))

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

(def ^:private long->short
  {:portal.colors/theme          :theme
   :portal.launcher/app          :app
   :portal.launcher/host         :host
   :portal.launcher/port         :port
   :portal.launcher/window-title :window-title})

(defn- rename [options]
  (set/rename-keys options long->short))

(defn set-defaults!
  "Set default options for `open`.
  Parameters passed directly to either will override defaults."
  {:added    "0.20.0"
   :see-also ["open"]}
  [options]
  (swap! rt/default-options merge (rename options)))

(defn ^:export open
  "Open a new inspector window."
  ([]
   (open nil))
  ([options]
   (open options (or js/window
                     (nth (js-arguments) 1))))
  ([options opener]
   (let [options (-> (if-not (string? options)
                       options
                       (edn/read-string options))
                     (assoc :opener opener))]
     (s/assert-options options)
     (l/open (rename options)))))

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
  "Evaluate ClojureScript source given as a string in the UI runtime."
  {:added "0.19.0"}
  ([code]
   (eval-str :all code nil))
  ([_portal code opts]
   (let [result (l/eval-str (assoc opts :code code))]
     (cond-> result (not (:verbose opts)) :value))))

(defn sessions
  "Get all current portal sessions."
  {:added "0.27.0"}
  []
  (c/sessions))

(defonce ^:private init? (atom false))

(defn- init []
  (when-not @init?
    (reset! init? true)
    (l/init @rt/default-options)
    (shortcuts/add!
     ::init
     (fn [log]
       (when (shortcuts/match?
              {::shortcuts/osx     #{"meta" "shift" "o"}
               ::shortcuts/default #{"control" "shift" "o"}}
              log)
         (open))))))

(js/setTimeout init 0)
