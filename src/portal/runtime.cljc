(ns portal.runtime
  (:refer-clojure :exclude [read])
  (:require [clojure.datafy :refer [datafy nav]]
            [portal.runtime.cson :as cson]
            #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a]))
  #?(:clj (:import [java.io File]
                   [java.net URI URL])))

(defonce ^:dynamic *options* nil)
(defonce ^:private id (atom 0))
(defn- next-id [] (swap! id inc))

(defonce request (atom nil))

(defn- set-timeout [f timeout]
  #?(:clj  (future (Thread/sleep timeout) (f))
     :cljs (js/setTimeout f timeout)))

(defn broadcast-change [_watch-key a old new]
  (when-not (= old new)
    (set-timeout
     #(when (= @a new)
        (when-let [request @request]
          (request {:op :portal.rpc/update-versions :body new})))
     100)))

(defn- atom? [o]
  #?(:clj  (instance? clojure.lang.Atom o)
     :cljs (satisfies? cljs.core/IAtom o)))

(defonce ^:private watch-registry (atom {}))
(add-watch watch-registry ::watch-key #'broadcast-change)

(defn- watch-atom [a]
  (when-not (contains? @watch-registry a)
    (swap!
     watch-registry
     (fn [atoms]
       (if (contains? atoms a)
         atoms
         (do
           (add-watch
            a
            ::watch-key
            (fn [_watch-key a _old _new]
              (swap! watch-registry update a inc)))
           (assoc atoms a 0)))))))

(defn- value->id [value]
  (let [k [:value value]]
    (-> (:value-cache *options*)
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [id (next-id)]
               (assoc cache [:id id] value k id)))))
        (get k))))

(defn- value->id? [value]
  (get @(:value-cache *options*) [:value value]))

(defn- id->value [id]
  (get @(:value-cache *options*) [:id id]))

(defn- to-object [value tag rep]
  (when (atom? value) (watch-atom value))
  (cson/tag
   "object"
   (cson/to-json
    {:id     (value->id value)
     :type   (pr-str (type value))
     :tag    tag
     :rep    rep
     :pr-str (binding [*print-length* 10
                       *print-level* 2]
               (pr-str value))})))

(extend-type #?(:clj Object :cljs default)
  cson/ToJson
  (-to-json [value]
    (to-object value :object nil)))

(defn- var->symbol [v]
  (let [m (meta v)]
    (symbol (str (:ns m)) (str (:name m)))))

#?(:bb (def clojure.lang.Var (type #'type)))

(extend-type #?(:clj  clojure.lang.Var
                :cljs cljs.core/Var)
  cson/ToJson
  (-to-json [value]
    (to-object value :var (var->symbol value))))

#?(:clj
   (extend-type clojure.lang.Ratio
     cson/ToJson
     (-to-json [value]
       (to-object value
                  :ratio
                  [(numerator value)
                   (denominator value)]))))

(extend-type #?(:clj  clojure.lang.IRecord
                :cljs cljs.core/IRecord)
  cson/ToJson
  (-to-json [value]
    (to-object value :record (into {} value))))

(defn limit-seq [value]
  (if-not (seq? value)
    value
    (let [m     (meta value)
          limit (get m ::more-limit 100)
          [realized remaining] (split-at limit value)]
      (with-meta
        realized
        (merge
         m
         (when (seq remaining)
           {::more #(limit-seq (with-meta remaining m))}))))))

(defn- can-meta? [value]
  #?(:clj  (instance? clojure.lang.IObj value)
     :cljs (implements? IMeta value)))

(defn- id-coll [value]
  (if-not (and (coll? value)
               (can-meta? value))
    value
    (if-let [id (value->id? value)]
      (cson/->Tagged "ref" id)
      (vary-meta value assoc ::id (value->id value)))))

(defn write [value options]
  (binding [*options* options]
    (cson/write
     value
     {:transform (comp limit-seq id-coll)})))

(defn- ref-> [value]
  (id->value (second value)))

(defn read [string options]
  (binding [*options* options]
    (cson/read
     string
     {:default-handler
      (fn [value]
        (case (first value)
          "ref"    (ref-> value)
          (cson/->Tagged (first value) (cson/json-> (second value)))))})))

(defonce state    (atom {:portal.launcher/window-title "portal"}))
(defonce tap-list (atom (list)))

(defn update-value [new-value]
  (swap! tap-list conj new-value))

(defn clear-values
  ([] (clear-values nil identity))
  ([_request done]
   (reset! id 0)
   (reset! tap-list (list))
   (reset! (:value-cache *options*) {})
   (doseq [[a] @watch-registry]
     (remove-watch a ::watch-key))
   (reset! watch-registry {})
   (done nil)))

(def ^:private predicates
  (merge
   {'clojure.core/deref
    #?(:clj  #(instance? clojure.lang.IRef %)
       :cljs #(satisfies? cljs.core/IDeref %))}
   #?(:clj
      {'clojure.core/slurp
       #(or (instance? URI %)
            (instance? URL %)
            (and (instance? File %)
                 (.isFile ^File %)
                 (.canRead ^File %)))})))

(def ^:private public-fns
  (merge
   {'clojure.core/pr-str   #'pr-str
    'clojure.core/deref    #'deref
    'clojure.core/type     #'type
    'clojure.datafy/datafy #'datafy}
   #?(:clj {`slurp slurp
            `bean  bean})))

(defn- get-functions [v]
  (keys
   (reduce-kv
    (fn [fns s predicate]
      (if (predicate v)
        fns
        (dissoc fns s)))
    public-fns
    predicates)))

(defn- get-tap-atom [] tap-list)

(defn- ping [] ::pong)

(def ^:private fns
  (merge
   public-fns
   {'clojure.datafy/nav  #'nav
    `ping                #'ping
    `get-tap-atom        #'get-tap-atom
    `clear-values        #'clear-values
    `get-functions       #'get-functions}))

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get fns f) f)]
      (a/let [return (apply f args)]
        (done {:return return})))
    (catch #?(:clj Exception :cljs js/Error) e
      (done {:return e}))))

(def ops {:portal.rpc/invoke #'invoke})
