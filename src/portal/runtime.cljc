(ns portal.runtime
  (:require [clojure.datafy :refer [datafy nav]]
            ;[org.httpkit.client :as client]
            #?(:clj  [portal.sync  :as a]
               :cljs [portal.async :as a]))
  #?(:clj (:import [java.util UUID])))

#?(:clj (def random-uuid #(UUID/randomUUID)))

(defonce instance-cache (atom {}))
(defonce state (atom nil))

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

(defn load-state [request done]
  (let [state-value @state
        id (:portal/state-id request)
        in-sync? (= id (:portal/state-id state-value))]
    (if-not in-sync?
      (done state-value)
      (let [watch-key (keyword (gensym))]
        (add-watch
         state
         watch-key
         (fn [_ _ _old _new]
           (done @state)))
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
  ;(prn (meta (first (:args request))))
  ;(prn (:args request))
  (let [[coll k v] (:args request)
        naved      (if coll (nav coll k v) v)]
    (if (= naved v)
      ; allow untransformed promises to pass freely
      (on-datafy naved done)
      ; wait for any newly returned promise to resolve
      (a/let [naved naved] (on-datafy naved done)))))

 ;:portal.rpc/http-request
 ;(fn [request done]
 ;  (done
 ;   {:response
 ;    @(client/request (get-in request [:body :request]))}))

(def ops
  {:portal.rpc/clear-values clear-values
   :portal.rpc/load-state   load-state
   :portal.rpc/on-nav       on-nav})
