(ns portal.api
  (:require #?(:clj
               [portal.runtime.jvm.commands])
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l])
            [clojure.set :as set]
            [portal.runtime :as rt]))

(defonce ^:private default-options (atom nil))

(defn set-defaults!
  "Set default options for `open` and `start`.
  Parameters passed directly to either will override defaults."
  {:added    "0.20.0"
   :see-also ["open" "start"]}
  [options]
  (swap! default-options merge options))

(defn submit
  "Tap target function.

  Usage:
    (add-tap #'portal.api/submit)
    (remove-tap #'portal.api/submit)"
  {:added "0.9.0"}
  [value]
  (rt/update-value value)
  nil)

(defn tap
  "Add portal as a tap> target."
  {:added         "0.1.0"
   :deprecated    "0.9"
   :superseded-by "submit"}
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

(defn start
  "Start the HTTP server with non-default options. Only use if you need
  control over the HTTP server."
  {:added "0.6.2"}
  [options]
  (l/start (rename (merge @default-options options))))

(defn open
  "Open a new inspector window. A previous instance can be passed as
  parameter to make sure it is open."
  {:added "0.1.0"}
  ([] (open nil))
  ([portal-or-options]
   (if (:session-id portal-or-options)
     (open portal-or-options nil)
     (open nil portal-or-options)))
  ([portal options]
   (l/open portal (rename (merge @default-options options)))))

(defn close
  "Close all current inspector windows."
  {:added "0.1.0"}
  []
  (l/close)
  nil)

(defn clear
  "Clear all values."
  {:added "0.1.0"}
  []
  (l/clear)
  nil)

(defn register!
  "Register a var with portal. For now, the var should be a 1 arity fn.

  Example: `(register! #'identity)`

  The function name and doc string will show up in the command palette."
  {:added "0.16.0"}
  [var]
  (rt/register! var)
  nil)

(defn eval-str
  "Evalute ClojureScript source given as a string in the UI runtime."
  {:added "0.19.0"}
  [source]
  (l/eval-str source))
