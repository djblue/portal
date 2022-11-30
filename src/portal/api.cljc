(ns portal.api
  (:require #?(:clj
               [portal.runtime.jvm.commands])
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l])
            #?(:clj  [portal.sync :as a]
               :cljs [portal.async :as a])
            #?(:clj  [clojure.java.io :as io]
               :cljs [portal.resources :as io])
            [clojure.set :as set]
            [portal.runtime :as rt]
            [portal.runtime.cson :as cson]))

(defn submit
  "Tap target function.

  Usage:

  ```clojure
  (add-tap #'portal.api/submit)
  (remove-tap #'portal.api/submit)
  ```"
  {:added "0.9.0"}
  [value]
  (rt/update-value value)
  nil)

(defn tap
  "Add portal as a `tap>` target."
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

(defn set-defaults!
  "Set default options for `open` and `start`.
  Parameters passed directly to either will override defaults."
  {:added    "0.20.0"
   :see-also ["open" "start"]}
  [options]
  (swap! rt/default-options merge (rename options)))

(defn start
  "Start the HTTP server with non-default options. Only use if you need
  control over the HTTP server."
  {:added "0.6.2"}
  [options]
  (l/start (rename options)))

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
   (l/open portal (rename options))))

(defn close
  "Close all current inspector windows."
  {:added "0.1.0"}
  ([]
   (l/close :all)
   nil)
  ([portal]
   (l/close portal)
   nil))

(defn clear
  "Clear all values."
  {:added "0.1.0"}
  ([]
   (l/clear :all)
   nil)
  ([portal]
   (l/clear portal)
   nil))

(defn register!
  "Register a var with portal. For now, the var should be a 1 arity fn.

  Example:

  ```clojure
  (register! #'identity)
  ```

  The function name and doc string will show up in the command palette."
  {:added "0.16.0"}
  [var]
  (rt/register! var)
  nil)

(defn eval-str
  "Evalute ClojureScript source given as a string in the UI runtime. The parameters:

   - portal: portal instance returned from `portal.api/open` or `:all`
   - code (string): the ClojureScript source
   - opts (map): evaluation options.
     - `:verbose` optional, return a map containing more info that just the value.
       - Defaults to false.
     - `:await`   - optional, await a promise result. Defaults to `false`."
  {:added "0.19.0"
   :see-also ["open"]}
  ([code]
   (eval-str :all code))
  ([portal code]
   (eval-str portal code nil))
  ([portal code opts]
   (a/let [result (l/eval-str portal (assoc opts :code code))]
     (cond-> result (not (:verbose opts)) :value))))

(defn sessions
  "Get all current portal sessions."
  {:added "0.27.0"}
  []
  (l/sessions))

(defn url
  "Get url for portal session."
  {:added "0.33.0"}
  [portal]
  (l/url portal))

(def ^:no-doc ^:dynamic *nrepl-init* nil)

(defn repl
  "Start a repl for the given Portal session."
  {:added "0.31.0"}
  [portal]
  (if *nrepl-init*
    (*nrepl-init* portal)
    (throw
     (ex-info
      "Please start nREPL with `portal.nrepl/wrap-repl` middleware to enable the portal subrepl."
      {:portal-instance    portal
       :missing-middleware 'portal.nrepl/wrap-repl}))))

(defn- get-docs []
  (cson/read
   #?(:clj  (slurp (io/resource "portal/docs.json"))
      :cljs (io/inline "portal/docs.json"))))

(defn docs
  "Open portal docs."
  {:added "035.0"
   :see-also ["open"]}
  ([]
   (docs nil))
  ([options]
   (open (assoc options :value (get-docs)))))
