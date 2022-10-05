(ns ^:no-doc portal.runtime
  (:refer-clojure :exclude [read])
  (:require #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a])
            [clojure.datafy :refer [datafy nav]]
            [portal.runtime.cson :as cson]))

(defonce default-options (atom nil))

(defonce ^:dynamic *session* nil)
(defn- next-id [] (swap! (:id *session*) inc))
(defonce sessions (atom {}))

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

(defn reset-session [{:keys [session-id value-cache watch-registry] :as session}]
  (reset! value-cache {})
  (doseq [a @watch-registry]
    (remove-watch a session-id))
  (reset! watch-registry #{})
  session)

(defonce request (atom nil))

(defn- set-timeout [f timeout]
  #?(:clj  (future (Thread/sleep timeout) (f))
     :cljs (js/setTimeout f timeout)))

(defn- atom? [o]
  #?(:clj  (instance? clojure.lang.Atom o)
     :cljs (satisfies? cljs.core/IAtom o)))

(defn- invalidate [session-id a old new]
  (when-not (= old new)
    (set-timeout
     #(when (= @a new)
        (when-let [request @request]
          (request session-id {:op :portal.rpc/invalidate :atom a})))
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

(defn- value->key
  "Include metadata when capturing values in cache."
  [value]
  [:value value (meta value) (type value)])

(defn- value->id [value]
  (let [k (value->key value)]
    (-> (:value-cache *session*)
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [id (next-id)]
               (assoc cache [:id id] value k id)))))
        (get k))))

(defn- value->id? [value]
  (get @(:value-cache *session*) (value->key value)))

(defn- id->value [id]
  (get @(:value-cache *session*) [:id id]))

(defn- deref? [value]
  #?(:clj  (instance? clojure.lang.IRef value)
     :cljs (satisfies? cljs.core/IDeref value)))

(defn- to-object [buffer value tag rep]
  (if-not *session*
    (cson/tag buffer "remote" (pr-str value))
    (let [m (meta value)]
      (when (atom? value) (watch-atom value))
      (cson/tag
       buffer
       "object"
       (cond-> {:tag       tag
                :id        (value->id value)
                :type      (pr-str (type value))
                :protocols (cond-> #{}
                             (deref? value) (conj :IDeref))}
         m   (assoc :meta m)
         rep (assoc :rep rep))))))

(extend-type #?(:clj Object :cljs default)
  cson/ToJson
  (-to-json [value buffer]
    (to-object buffer value :object nil)))

#?(:bb (def clojure.lang.Range (type (range 1.0))))

(defn- can-meta? [value]
  #?(:clj  (and
            (not (instance? clojure.lang.Range value))
            (or (instance? clojure.lang.IObj value)
                (var? value)))
     :cljs (implements? IMeta value)))

(defn- has? [m k]
  (try
    (k m)
    (catch #?(:clj Exception :cljs :default) _e)))

(defn- no-cache [value]
  (or (not (coll? value))
      (empty? value)
      (cson/tagged-value? value)
      (not (can-meta? value))
      (has? value :portal.rpc/id)))

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
      (cson/tagged-value "var" (var->symbol value))
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

(defonce ^:private tap-list (atom (list)))

(defn update-value [new-value]
  (swap! tap-list conj new-value))

(defn- get-options []
  (let [options (:options *session*)]
    (merge
     {:name (if (= :dev (:mode options))
              "portal-dev"
              "portal")
      :version "0.31.0"
      :platform
      #?(:bb   "bb"
         :clj  "jvm"
         :cljs (cond
                 (exists? js/process)        "node"
                 (exists? js/PLANCK_VERSION) "planck"
                 :else                        "web"))
      :value tap-list}
     options)))

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
   (done nil)))

(defn- cache-evict [id]
  (let [value (id->value id)
        {:keys [session-id value-cache watch-registry]} *session*]
    (when (atom? value)
      (swap! watch-registry dissoc value)
      (remove-watch value session-id))
    (swap! value-cache dissoc [:id id] (value->key value))
    nil))

(defn update-selected
  ([value]
   (update-selected (:session-id *session*) value))
  ([session-id value]
   (swap! sessions assoc-in [session-id :selected] value)
   nil))

(def ^:private registry (atom {}))

(defn- get-functions [v]
  (keep
   (fn [[name opts]]
     (let [m      (merge (meta (:var opts)) opts)
           result (merge {:name name}
                         (select-keys m [:doc :command]))]
       (when-not (:private m)
         (if-let [predicate (:predicate m)]
           (try
             (when (predicate v) result)
             (catch #?(:clj Exception :cljs :default) _ex))
           result))))
   @registry))

(defn- ping [] ::pong)

(defn- get-function [f]
  (get-in @registry [f :var]))

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get-function f) f)]
      (a/let [return (apply f args)]
        (done {:return return})))
    (catch #?(:clj Exception :cljs js/Error) e
      (done {:error
             (-> (ex-info
                  "invoke exception"
                  {:function f
                   :args     args
                   :found?   (boolean (get-function f))}
                  e)
                 datafy
                 (assoc :runtime #?(:bb :bb :clj :clj :cljs :cljs)))}))))

(def ops {:portal.rpc/invoke #'invoke})

(def aliases {"cljs.core" "clojure.core"})

(defn- var->name [var]
  (let [{:keys [name ns]} (meta var)
        ns                (str ns)]
    (symbol (aliases ns ns) (str name))))

(defn register!
  ([var] (register! var {}))
  ([var opts]
   (swap! registry
          assoc
          (or (:name opts) (var->name var))
          (merge {:var var} opts))))

(doseq [var [#'ping
             #'cache-evict
             #'get-options
             #'get-functions
             #'type
             #'datafy]]
  (register! var))

(doseq [[var opts] {#'pr-str          {:name 'clojure.core/pr-str}
                    #'deref           {:name 'clojure.core/deref :predicate deref?}
                    #'meta            {:predicate can-meta?}
                    #'update-selected {:private true}
                    #'clear-values    {:private true}
                    #'nav             {:private true}}]
  (register! var opts))
