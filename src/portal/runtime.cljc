(ns portal.runtime
  (:require [clojure.datafy :refer [datafy nav]]
            #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a]))
  #?(:clj (:import [java.util UUID])))

#?(:clj (def random-uuid #(UUID/randomUUID)))

(defonce instance-cache (atom {}))

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

(defn uuid->instance [uuid]
  (get @instance-cache [:uuid uuid]))

(defonce state (atom {:portal/state-id (random-uuid)
                      :portal/value (list)}))

(defn update-setting [k v] (swap! state assoc k v) true)

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

(defn on-datafy [value done]
  (let [datafied (datafy value)]
    (if (= datafied value)
      ; allow untransformed promises to pass freely
      (done {:value datafied})
      ; wait for any newly returned promise to resolve
      (a/let [datafied datafied] (done {:value datafied})))))

(defn on-nav [request done]
  (let [[coll k v] (:args request)
        naved      (if coll (nav coll k v) v)]
    (if (= naved v)
      ; allow untransformed promises to pass freely
      (on-datafy naved done)
      ; wait for any newly returned promise to resolve
      (a/let [naved naved] (on-datafy naved done)))))

(defn invoke [{:keys [f args]} done]
  (try
    (done {:return (apply f args)})
    (catch #?(:clj Exception :cljs js/Error) e
      (done {:return e}))))

(def ops
  {:portal.rpc/clear-values clear-values
   :portal.rpc/load-state   load-state
   :portal.rpc/on-nav       on-nav
   :portal.rpc/invoke       invoke})
