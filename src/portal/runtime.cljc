(ns portal.runtime
  (:refer-clojure :exclude [read])
  (:require [clojure.datafy :refer [datafy nav]]
            [portal.runtime.cson :as cson]
            #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a]))
  #?(:clj (:import [java.io File]
                   [java.net URI URL]
                   [java.util UUID])))

#?(:clj (defn random-uuid [] (UUID/randomUUID)))

(defonce instance-cache (atom {}))
(defonce ^:private id (atom 0))
(defn- next-id [] (swap! id inc))

(defonce request (atom nil))

(defn instance->uuid [instance]
  (let [k [:instance instance]]
    (-> instance-cache
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [id (next-id)]
               (assoc cache [:id id] instance k id)))))
        (get k))))

(defn uuid->instance [uuid]
  (get @instance-cache [:id uuid]))

(extend-type #?(:clj Object :cljs default)
  cson/ToJson
  (-to-json [o]
    (let [id (instance->uuid o)]
      (cson/tag
       "object"
       (cson/to-json
        {:id id
         :type (pr-str (type o))
         :string (binding [*print-length* 10
                           *print-level* 2]
                   (pr-str o))})))))

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

(defn write [value]
  (cson/write value limit-seq))

(defn- object-> [value]
  (let [rep (cson/json-> (second value))]
    (uuid->instance (:id rep))))

(defn read [string]
  (cson/read
   string
   (fn [value]
     (case (first value)
       "object" (object-> value)))))

(defonce state (atom {:portal/state-id (random-uuid)
                      :portal/tap-list (list)
                      :portal.launcher/window-title "portal"}))

(defn update-value [new-value]
  (swap!
   state
   (fn [state]
     (assoc
      state
      :portal/state-id (random-uuid)
      :portal/tap-list
      (let [value (:portal/tap-list state)]
        (if-not (coll? value)
          (list new-value)
          (conj value new-value)))))))

(defn clear-values
  ([] (clear-values nil identity))
  ([_request done]
   (reset! id 0)
   (reset! instance-cache {})
   (swap! state assoc
          :portal/state-id (random-uuid)
          :portal/tap-list (list))
   (done nil)))

(defn- set-limit [state]
  (update state
          :portal/tap-list
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
   {'clojure.core/deref    #'deref
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

(defn- ping [] ::pong)

(def ^:private fns
  (merge
   public-fns
   {'clojure.datafy/nav  #'nav
    `ping                #'ping
    `clear-values        #'clear-values
    `load-state          #'load-state
    `get-functions       #'get-functions}))

(defn invoke [{:keys [f args]} done]
  (try
    (let [f (if (symbol? f) (get fns f) f)]
      (a/let [return (apply f args)]
        (done {:return return})))
    (catch #?(:clj Exception :cljs js/Error) e
      (done {:return e}))))

(def ops
  {:portal.rpc/load-state   #'load-state
   :portal.rpc/invoke       #'invoke})
