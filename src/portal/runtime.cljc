(ns portal.runtime
  (:require [clojure.datafy :refer [datafy nav]])
  #?(:clj (:import [java.io File]
                   [java.net URI URL]
                   [java.util UUID])))

#?(:clj (defn random-uuid [] (UUID/randomUUID)))

(defonce instance-cache (atom {}))

(declare object->value)

(defn instance->uuid [instance]
  (let [k [:instance instance]]
    (-> instance-cache
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [uuid (random-uuid)]
               (assoc cache [:uuid uuid] instance k uuid)))))
        (get k))))

(defn- can-meta? [o]
  #?(:clj (instance? clojure.lang.IObj o)
     :cljs (implements? IMeta o)))

(defn object->value
  ([o]
   (object->value o (instance->uuid o)))
  ([o id]
   {:id id
    :meta (when (can-meta? o) (meta o))
    :type (pr-str (type o))
    :string (binding
             [*print-length* 10
              *print-level* 2]
              (pr-str o))}))

(defn uuid->instance [uuid]
  (get @instance-cache [:uuid uuid]))

(defonce state (atom {:portal/state-id (random-uuid)
                      :portal/value (list)}))

(defn update-value [new-value]
  (swap!
   state
   (fn [state]
     (assoc
      state
      :portal/state-id (random-uuid)
      :portal/value
      (let [value (:portal/value state)]
        (if-not (coll? value)
          (list new-value)
          (conj value new-value)))))))

(defn clear-values
  ([] (clear-values nil identity))
  ([_request done]
   (reset! instance-cache {})
   (swap! state assoc
          :portal/state-id (random-uuid)
          :portal/value (list))
   (done nil)))

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

(defn- set-limit [state]
  (update state
          :portal/value
          #(with-meta % {::more-limit 25})))

(defn load-state [request done]
  (let [state-value @state
        id (:portal/state-id request)
        in-sync? (= id (:portal/state-id state-value))]
    (if-not in-sync?
      (done (set-limit state-value))
      (let [watch-key (keyword (gensym))]
        (add-watch
         state
         watch-key
         (fn [_ _ _old _new]
           (remove-watch state watch-key)
           (done (set-limit @state))))
        (fn [_status]
          (remove-watch state watch-key))))))

(defn on-nav [request done]
  (let [[coll k v] (:args request)]
    (done {:value (if coll (nav coll k v) v)})))

(def ^:private predicates
  (merge
   {'clojure.core/deref
    #?(:clj  #(instance? clojure.lang.IRef %)
       :cljs #(satisfies? cljs.core.IDeref %))}
   #?(:clj
      {'clojure.core/slurp
       #(or (instance? URI %)
            (instance? URL %)
            (and (instance? File %)
                 (.isFile ^File %)
                 (.canRead ^File %)))})))

(declare get-functions)

(def ^:private fns
  (merge
   {'clojure.core/deref    #'deref
    'clojure.core/type     #'type
    'clojure.datafy/datafy #'datafy
    `get-functions #'get-functions}
   #?(:clj {`slurp slurp
            `bean  bean})))

(defn- get-functions [v]
  (keys
   (reduce-kv
    (fn [fns s predicate]
      (if (predicate v)
        fns
        (dissoc fns s)))
    (dissoc fns `get-functions)
    predicates)))

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get fns f) f)]
      (done {:return (apply f args)}))
    (catch #?(:clj Exception :cljs js/Error) e
      (done {:return e}))))

(def ops
  {:portal.rpc/clear-values #'clear-values
   :portal.rpc/load-state   #'load-state
   :portal.rpc/on-nav       #'on-nav
   :portal.rpc/invoke       #'invoke})
