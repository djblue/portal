(ns portal.runtime
  (:require [clojure.datafy :refer [datafy nav]]
            [org.httpkit.client :as client])
  (:import [java.util UUID]))

(defonce instance-cache (atom {}))
(defonce state (atom nil))

(defn update-setting [k v] (swap! state assoc k v) true)

(defn update-value [new-value]
  (swap!
   state
   (fn [state]
     (assoc
      state
      :portal/state-id (UUID/randomUUID)
      :portal/value
      (let [value (:portal/value state)]
        (if-not (coll? value)
          (list new-value)
          (conj value new-value)))))))

(defn clear-values []
  (reset! instance-cache {})
  (swap! state assoc
         :portal/state-id (UUID/randomUUID)
         :portal/value (list)))

(def ops
  {:portal.rpc/clear-values
   (fn [_request done]
     (done (clear-values)))
   :portal.rpc/load-state
   (fn [request done]
     (let [state-value @state
           id (get-in request [:body :portal/state-id])]
       (if-not (= id (:portal/state-id state-value))
         (done state-value)
         (let [watch-key (keyword (gensym))]
           (add-watch
            state
            watch-key
            (fn [_ _ _old _new]
              (done @state)))
           (fn [_status]
             (remove-watch state watch-key))))))
   :portal.rpc/http-request
   (fn [request done]
     (done
      {:response
       @(client/request (get-in request [:body :request]))}))
   :portal.rpc/on-nav
   (fn [request done]
     (done
      {:value (datafy (apply nav (get-in request [:body :args])))}))})

