(ns portal.api
  (:require [portal.runtime :as rt]
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l])
            #?(:clj
               [portal.runtime.jvm.commands])))

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

(defn start
  "Start the HTTP server with non-default options. Only use if you need
  control over the HTTP server."
  {:added "0.6.2"}
  [options]
  (l/start options))

(defn open
  "Open a new inspector window. A previous instance can be passed as
  parameter to make sure it is open."
  {:added "0.1.0"}
  ([] (open nil))
  ([portal-or-options]
   (if (:session-id portal-or-options)
     (l/open portal-or-options nil)
     (l/open nil portal-or-options))))

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
