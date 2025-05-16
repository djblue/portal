(ns ^:no-doc portal.runtime
  (:refer-clojure :exclude [read])
  (:require #?(:clj  [portal.sync  :as a]
               :cljr [portal.sync  :as a]
               :cljs [portal.async :as a]
               :lpy  [portal.sync  :as a])
            #?(:joyride [portal.runtime.datafy :refer [datafy nav]]
               :org.babashka/nbb [portal.runtime.datafy :refer [datafy nav]]
               :lpy     [portal.runtime.datafy :refer [datafy nav]]
               :default [clojure.datafy :refer [datafy nav]])
            #?(:joyride [cljs.pprint :as pprint]
               :default [clojure.pprint :as pprint])
            [portal.runtime.cson :as cson]
            [portal.viewer :as v])
  #?(:lpy (:import [asyncio :as asyncio])))

(def ^:private tagged-type (type (cson/->Tagged "tag" [])))

#?(:joyride nil
   :org.babashka/nbb nil
   :lpy nil
   :default
   (defmethod pprint/simple-dispatch tagged-type [value]
     (if (not= (:tag value) "remote")
       (pr value)
       (print (:rep value)))))

(defonce default-options (atom nil))

(defonce ^:dynamic *session* nil)

(defonce sessions (atom (v/table {} {:columns [:options :selected]})))
(defonce connections (atom {}))
(defonce pending-requests (atom {}))

(defn active-sessions [] (keys @connections))

(defn cleanup-sessions []
  (swap! sessions select-keys (keys @connections)))

(defonce id (atom 0))

(defn next-id
  ([] (swap! id inc))
  ([{:keys [id]}] (swap! id inc)))

(defn get-session [session-id]
  (-> @sessions
      (get session-id)
      (assoc :session-id session-id)
      (update :options #(merge @default-options %))))

(defn open-session [{:keys [session-id] :as session}]
  (merge
   (get-session session-id)
   {:id             (atom 0)
    :value-cache    (atom {})
    :watch-registry (atom #{})}
   session))

(defn close-session [session-id]
  (swap! sessions dissoc session-id))

(defn reset-session [{:keys [session-id value-cache watch-registry] :as session}]
  (reset! value-cache {})
  (doseq [a @watch-registry]
    (remove-watch a session-id))
  (reset! watch-registry #{})
  session)

(defonce request (atom nil))

#?(:lpy (defonce ^:no-doc async-loop (atom nil)))

(defn- set-timeout [f ^long timeout]
  #?(:clj  (future (Thread/sleep timeout) (f))
     :cljr (future (System.Threading.Thread/Sleep timeout) (f))
     :cljs (js/setTimeout f timeout)
     :lpy  (asyncio/run_coroutine_threadsafe
            (^:async
             (fn []
               (await (asyncio/sleep (/ timeout 1000.0)))
               (future (f))))
            @async-loop)))

(defn- hashable? [value]
  (try
    (and (hash value) true)
    (catch #?(:cljs :default :default Exception) _
      false)))

#?(:bb (def clojure.lang.Range (type (range 1.0))))

(defn- can-meta? [value]
  #?(:clj  (and
            (not (instance? clojure.lang.Range value))
            (or (instance? clojure.lang.IObj value)
                (var? value)))
     :cljr (or (instance? clojure.lang.IObj value)
               (var? value))
     :joyride
     (and (some? value)
          (try (with-meta value {}) true
               (catch :default _e false)))

     :org.babashka/nbb
     (and (some? value)
          (try (with-meta value {}) true
               (catch :default _e false)))

     :cljs (implements? IMeta value)

     :lpy
     (try (with-meta value {}) true
          (catch Exception _e false))))

#?(:lpy (defn- sorted? [_] false))

(defn- hash+ [x]
  (cond
    (map? x)
    (reduce-kv
     (fn [out k v]
       (+ out (hash+ k) (hash+ v)))
     (+ 1 (if (sorted? x) 1 0) (hash+ (meta x)))
     x)

    (coll? x)
    (reduce
     (fn [out v]
       (+ out (hash+ v)))
     (+ (cond
          (list? x)   3
          (set? x)    (if (sorted? x) 4 5)
          (vector? x) 6
          :else       7)
        (hash+ (meta x)))
     x)

    :else
    (cond-> (+ (hash x) (hash (type x)))
      (can-meta? x)
      (+ (hash+ (meta x))))))

(defn- value->key
  "Include metadata when capturing values in cache."
  [value]
  (when (hashable? value)
    [:value value (hash+ value)]))

#?(:joyride (def Atom (type (atom nil))))
#?(:org.babashka/nbb (def Atom (type (atom nil))))

(defn- atom? [o]
  #?(:clj  (instance? clojure.lang.Atom o)
     :cljr (instance? clojure.lang.Atom o)
     :joyride (= Atom (type o))
     :org.babashka/nbb (= Atom (type o))
     :cljs (satisfies? cljs.core/IAtom o)
     :lpy (instance? basilisp.lang.atom/Atom o)))

(defn- notify [session-id a]
  (when-let [request @request]
    (request session-id {:op :portal.rpc/invalidate :atom a})))

(defn- invalidate [session-id a old new]
  (when-not (= (value->key old) (value->key new))
    (set-timeout
     #(when (= @a new) (notify session-id a))
     100)))

(defn- watch-atom [a]
  (let [{:keys [session-id watch-registry]} *session*]
    (when-not (contains? @watch-registry a)
      (swap!
       watch-registry
       (fn [atoms]
         (if (contains? atoms a)
           atoms
           (do
             (add-watch a session-id #'invalidate)
             (conj atoms a))))))))

(defn- toggle-watch
  "Toggle watching an atom for a given Portal session."
  {:command true}
  [a]
  (let [{:keys [session-id watch-registry]} *session*]
    (when
     (contains?
      (swap!
       watch-registry
       (fn [atoms]
         (if (contains? atoms a)
           (do
             (remove-watch a session-id)
             (disj atoms a))
           (do
             (add-watch a session-id #'invalidate)
             (conj atoms a))))) a)
      (set-timeout #(notify session-id a) 0))))

(defn- value->id [value]
  (let [k   (value->key value)
        out (atom nil)]
    (swap!
     (:value-cache *session*)
     (fn [cache]
       (if-let [id (and k (get cache k))]
         (do (reset! out id) cache)
         (let [id (next-id *session*)]
           (reset! out id)
           (cond-> (assoc cache [:id id] value) k (assoc k id))))))
    @out))

(defn- value->id? [value]
  (get @(:value-cache *session*) (value->key value)))

(defn- id->value [id]
  (get @(:value-cache *session*) [:id id]))

(defn- deref? [value]
  #?(:clj  (instance? clojure.lang.IRef value)
     :cljr (instance? clojure.lang.IRef value)
     :cljs (satisfies? cljs.core/IDeref value)
     :lpy  (atom? value)))

(defn- pr-str' [value]
  (try
    (if-not (deref? value)
      (pr-str value)
      (str "#object " (pr-str [(type value) {:val ::elided}])))
    (catch #?(:cljs :default :default Exception) _
      (str "#object [" (pr-str (type value)) " unprintable]"))))

(defn- to-object [buffer value tag rep]
  (if-not *session*
    (cson/to-json*
     (with-meta
       (cson/tagged-value "remote" (pr-str value))
       (meta value))
     buffer)
    (let [m (meta value)]
      (when (atom? value) (watch-atom value))
      (cson/tag
       buffer
       "object"
       (cond-> {:tag       tag
                :id        (value->id value)
                :type      (pr-str (type value))
                :pr-str    (pr-str' value)
                :protocols (cond-> #{}
                             (deref? value) (conj :IDeref))}
         m   (assoc :meta m)
         rep (assoc :rep rep))))))

#?(:bb nil
   :clj
   (extend-type java.util.Collection
     cson/ToJson
     (to-json* [value buffer]
       (if-let [id (value->id? value)]
         (cson/to-json* (cson/tagged-value "ref" id) buffer)
         (cson/tagged-coll
          buffer
          (cond
            (instance? java.util.Set value)          "#"
            (instance? java.util.RandomAccess value) "["
            :else                                    "(")
          {::id (value->id value) ::type (type value)}
          value)))))

#?(:bb nil
   :clj
   (extend-type java.util.Map
     cson/ToJson
     (to-json* [value buffer]
       (if-let [id (value->id? value)]
         (cson/to-json* (cson/tagged-value "ref" id) buffer)
         (cson/tagged-map
          buffer
          "{"
          (if (record? value)
            (meta value)
            {::id (value->id value) ::type (type value)})
          value)))))

(extend-type #?(:clj  Object
                :cljr Object
                :cljs default
                :lpy  python/object)
  cson/ToJson
  (to-json* [value buffer]
    (to-object buffer value :object nil)))

(defn- has? [m k]
  (try
    (k m)
    (catch #?(:cljs :default :default Exception) _e nil)))

(defn- no-cache [value]
  (or (not (coll? value))
      (empty? value)
      (cson/tagged-value? value)
      (not (can-meta? value))
      (has? value ::id)
      (has? value :portal.rpc/id)
      (::no-cache (meta value))))

(defn- id-coll [value]
  (if (no-cache value)
    value
    (if-let [id (value->id? value)]
      (cson/tagged-value "ref" id)
      (vary-meta value
                 merge
                 (cond-> {::id (value->id value)}
                   (record? value)
                   (assoc ::type (type value)))))))

(defn- var->symbol [v]
  (let [m (meta v)]
    (symbol (str (:ns m)) (str (:name m)))))

(defn- id-var [value]
  (if-not (var? value)
    value
    (with-meta
      (cson/tagged-value "portal/var" (var->symbol value))
      (assoc (meta value) ::id (value->id value)))))

(defn write [value session]
  (binding [*session* session]
    (cson/write
     value
     (merge
      session
      {:transform (comp id-var id-coll)
       :to-object to-object}))))

(defn read [string session]
  (binding [*session* session]
    (cson/read
     string
     (merge
      session
      {:default-handler
       (fn [op value]
         (case op
           "ref" (id->value value)
           (cson/tagged-value op value)))}))))

(defonce ^:private tap-list
  (atom (with-meta (list)
          {:portal.viewer/default :portal.viewer/inspector})))

(comment
  (reset! tap-list
          (with-meta (list)
            {:portal.viewer/default :portal.viewer/inspector}))
  (meta @tap-list)
  (tap> :hi))

(defn- realize-value! [x]
  (cond
    (map? x)
    (doseq [[k v] x]
      (realize-value! k)
      (realize-value! v))
    (coll? x)
    (doseq [v (take 100000 x)]
      (realize-value! v))))

#_{:clj-kondo/ignore [:unused-private-var]}
(defn- runtime []
  #?(:portal :portal :bb :bb :clj :clj :joyride :joyride :org.babashka/nbb :nbb :cljs :cljs :cljr :cljr :lpy :py))

(defn- error->data [e]
  #?(:clj  (assoc (Throwable->map e) :runtime (runtime))
     :cljr (assoc (Throwable->map e) :runtime (runtime))
     :default e))

(defn update-value [new-value]
  (try
    (realize-value! new-value)
    (swap! tap-list conj new-value)
    (catch #?(:cljs :default :default Exception) e
      (swap! tap-list conj
             (error->data
              #?(:lpy
                 (ex-info "Failed to receive value." {:value-type (type new-value)})
                 :default
                 (ex-info "Failed to receive value." {:value-type (type new-value)} e)))))))

(def ^:private runtime-keymap (atom ^::no-cache {}))

(defn- get-options []
  (let [{:keys [options watch-registry]} *session*]
    (with-meta
      (merge
       {:name (if (= :dev (:mode options))
                "portal-dev"
                "portal")
        :version "0.59.2"
        :runtime (runtime)
        :platform
        #?(:bb   "bb"
           :clj  "jvm"
           :cljr "clr"
           :lpy  "py"
           :joyride "joyride"
           :org.babashka/nbb "nbb"
           :cljs (cond
                   (exists? js/window)         "web"
                   (exists? js/process)        "node"
                   (exists? js/PLANCK_VERSION) "planck"
                   :else                        "web"))
        :value tap-list
        :keymap runtime-keymap
        :watch-registry watch-registry}
       options)
      {::no-cache true})))

(defn clear-values
  ([] (clear-values nil identity))
  ([_request done]
   (when-let [{:keys [session-id value-cache watch-registry]} *session*]
     (let [value (:value (get-options))]
       (when (atom? value)
         (swap! value empty)))
     (reset! value-cache {})
     (doseq [a @watch-registry]
       (remove-watch a session-id))
     (reset! watch-registry #{}))
   (cleanup-sessions)
   (done nil)))

(defn- cache-evict [id]
  (let [value (id->value id)
        {:keys [session-id value-cache watch-registry]} *session*]
    (when (atom? value)
      (swap! watch-registry disj value)
      (remove-watch value session-id))
    (swap! value-cache dissoc [:id id] (value->key value))
    nil))

(defn update-selected
  ([value]
   (update-selected (:session-id *session*) value))
  ([session-id value]
   (swap! sessions assoc-in [session-id :selected] value)
   nil))

(def ^:private registry (atom (v/table {} {:columns [:var :predicate :private]})))

(defn- get-functions [v]
  (-> (reduce-kv
       (fn [out name opts]
         (let [m      (merge (meta (:var opts)) opts)
               result (-> (select-keys m [:doc :command])
                          (assoc :name name)
                          (vary-meta assoc ::no-cache true))]
           (if (:private m)
             out
             (if-let [predicate (:predicate m)]
               (try
                 (cond-> out
                   (predicate v)
                   (assoc name result))
                 (catch #?(:cljs :default :default Exception) _ex out))
               (assoc out name result)))))
       {}
       @registry)
      (v/table {:columns [:doc :command]})
      (vary-meta assoc ::no-cache true)))

(defn- ping [] ::pong)

(defn- get-function [f]
  (get-in @registry [f :var]))

(defn- source-info [f]
  (when (can-meta? f)
    (select-keys (meta f) [:file :line :column])))

(defn invoke [{:keys [f args]} done]
  (let [session *session*
        f (if (symbol? f) (get-function f) f)]
    (a/try
      (a/let [return (binding [*session* session] (apply f args))]
        (done (assoc (source-info f) :return return)))
      (catch #?(:cljs :default :default Exception) e
        (done (assoc
               (source-info f)
               :error
               (-> (ex-info
                    (ex-message e)
                    {::function f
                     ::args     args
                     ::found?   (some? f)
                     ::data     (ex-data e)})
                   datafy
                   (assoc :runtime (runtime)))))))))

(def ops {:portal.rpc/invoke #'invoke})

(def aliases {"cljs.core" "clojure.core"})

(defn- var->name [var]
  (let [{:keys [name ns]} (meta var)
        ns                (str ns)]
    (symbol (aliases ns ns) (str name))))

(defn register!
  ([var] (register! var {}))
  ([var opts]
   (let [m    (meta var)
         name (or (:name opts) (var->name var))]
     (doseq [shortcut (concat (:shortcuts m) (:shortcuts opts))]
       (swap! runtime-keymap assoc shortcut name))
     (swap! registry
            assoc
            name
            (merge {:var var} opts)))))

(doseq [var [#'ping
             #'cache-evict
             #'get-options
             #'get-functions
             #'type]]
  (register! var))

(doseq [[var opts] {#'pr-str          {:name 'clojure.core/pr-str}
                    #'deref           {:name 'clojure.core/deref :predicate deref?}
                    #'meta            {:predicate can-meta?}
                    #'update-selected {:private true}
                    #'clear-values    {:private true}
                    #'nav             {:name 'clojure.datafy/nav
                                       :private true
                                       :shortcuts [#{"enter"}]}
                    #'datafy          {:name 'clojure.datafy/datafy
                                       :shortcuts [#{"shift" "enter"}]}
                    #'toggle-watch    {:private false
                                       :predicate atom?
                                       :name 'portal.api/toggle-watch}}]
  (register! var opts))
