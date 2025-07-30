(ns portal.api
  (:require #?(:clj
               [portal.runtime.jvm.commands])
            #?(:clj  [portal.runtime.jvm.launcher :as l]
               :cljs [portal.runtime.node.launcher :as l]
               :cljr [portal.runtime.clr.launcher :as l]
               :lpy  [portal.runtime.python.launcher :as l])
            #?(:clj  [portal.sync :as a]
               :cljs [portal.async :as a]
               :cljr [portal.sync :as a]
               :lpy  [portal.sync :as a])
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
  (set/rename-keys (or options {}) long->short))

(defn set-defaults!
  "Set default options for `open` and `start`.
  Parameters passed directly to either will override defaults."
  {:added    "0.20.0"
   :see-also ["open" "start"]}
  [options]
  (swap! rt/default-options merge (rename options)))

(defn start
  "Start the HTTP server with non-default options. Only use if you need
  control over the HTTP server.

  - options (map): start options.
    - `:host` (string) optional, server host to use for Portal server.
      - Ignored if server already started.
    - `:port` (number) optional, server port to use for Portal server.
      - Ignored if server already started."
  {:added "0.6.2"
   :see-also ["stop"]}
  [options]
  (l/start (rename options)))

(defn stop
  "Stop the HTTP server."
  {:added "0.41.0"
   :see-also ["start"]}
  []
  (l/stop))

(defn open
  "Open a new inspector window. A previous instance can be passed as
  parameter to make sure it is open.

  - options (map): open options.
    - `:theme` (keyword) theme to use for Portal UI, see `portal.colors/themes` for themes.
      - Themes are applied per UI instance, not globally.
    - `:window-title` (string) browser window title.
      - Useful for distinguishing multiple UI instances.
    - `:app` (boolean) Run in `--app` mode via chrome.
      - Defaults to true when chrome is installed.
      - Will use PWA at https://djblue.github.io/portal/ when installed."
  {:added "0.1.0"
   :see-also ["close"]}
  ([] (open nil))
  ([portal-or-options]
   (if (:session-id portal-or-options)
     (open portal-or-options nil)
     (open nil portal-or-options)))
  ([portal options]
   (l/open portal
           (merge
            (dissoc (:options rt/*session*) :value)
            (rename options)))))

(defn close
  "Close all current inspector windows.

   - portal: Portal session returned via `portal.api/open`.
   "
  {:added "0.1.0"
   :see-also ["open"]}
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
  {:added "0.16.0"
   :command true
   :predicate var?}
  [var]
  (rt/register! var)
  nil)

(register! #'register!)

(defn- print-err [s]
  #?(:clj  (binding [*out* *err*]
             (print s)
             (flush))
     :cljs (binding [*print-fn* *print-err-fn*]
             (print s)
             (flush))
     :cljr (binding [*out* *err*]
             (print s)
             (flush))))

(defn eval-str
  "Evaluate ClojureScript source given as a string in the UI runtime. The parameters:

   - portal: portal instance returned from `portal.api/open` or `:all`
   - code (string): the ClojureScript source
   - opts (map): evaluation options.
     - `:verbose` (boolean) optional, return a map containing more info that just the value.
       - Defaults to `false`.
     - `:await`   (boolean) optional, await a promise result.
       - Defaults to `false`."
  {:added "0.19.0"
   :see-also ["open"]}
  ([code]
   (eval-str :all code))
  ([portal code]
   (eval-str portal code nil))
  ([portal code opts]
   (a/let [result (l/eval-str portal (assoc opts :code code))]
     (when-not (:verbose opts)
       (doseq [{:keys [tag val]} (:stdio result)]
         (cond
           (= :out tag) (do (print val) (flush))
           (= :err tag) (print-err val))))
     (cond-> result (not (:verbose opts)) :value))))

(defn sessions
  "Get all current portal sessions."
  {:added "0.27.0"}
  []
  (l/sessions))

(defn url
  "Get url for portal session.

   - portal: Portal session returned via `portal.api/open`"
  {:added "0.33.0"}
  [portal]
  (l/url portal))

(def ^:no-doc ^:dynamic *nrepl-init* nil)

(defn repl
  "Start a repl for the given Portal session.

  - portal: Portal session returned via `portal.api/open`"
  {:added "0.31.0"}
  ([]
   (repl :all))
  ([portal]
   (if *nrepl-init*
     (*nrepl-init* portal)
     (throw
      (ex-info
       "Please start nREPL with `portal.nrepl/wrap-repl` middleware to enable the portal subrepl."
       {:portal-instance    portal
        :missing-middleware 'portal.nrepl/wrap-repl})))))

#?(:cljs (def ^:private docs-json (io/inline "portal/docs.json")))

(defn- get-docs []
  (cson/read
   #?(:clj  (slurp (io/resource "portal/docs.json"))
      :cljs docs-json)))

(defn docs
  "Open portal docs.

  - options: see `portal.api/open` for options."
  {:added "035.0"
   :see-also ["open"]}
  ([]
   (docs nil))
  ([options]
   (open (assoc options :window-title "portal-docs" :value (get-docs)))))

(defn inspect
  "Open a new portal window to inspect a particular value.

   - value: a value to inspect.
   - options: see `portal.api/open` for options."
  {:command true
   :added "0.38.0"
   :see-also ["open"]}
  ([value]
   (open {:value value}))
  ([value options]
   (open (assoc options :value value))))

(register! #'inspect)

(defn selected
  "Get a sequence of all currently selected values."
  {:added "0.49.0"}
  ([]
   (mapcat selected (sessions)))
  ([session]
   (get-in @rt/sessions [(:session-id session) :selected])))
