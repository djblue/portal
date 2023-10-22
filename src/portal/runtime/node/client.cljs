(ns ^:no-doc portal.runtime.node.client
  (:require [portal.async :as a]
            [portal.runtime :as rt]))

(def ops
  {:portal.rpc/response
   (fn [message _done]
     (let [id (:portal.rpc/id message)]
       (when-let [[resolve] (get @rt/pending-requests id)]
         (resolve message))))})

(def timeout 60000)

(defn- get-connection [session-id]
  (let [done (atom nil)]
    (.race
     js/Promise
     [(js/Promise.
       (fn [resolve _reject]
         (let [handle (js/setTimeout #(resolve nil) timeout)]
           (reset! done #(js/clearTimeout handle)))))
      (js/Promise.
       (fn [resolve _reject]
         (if-let [send! (get @rt/connections session-id)]
           (do (@done) (resolve send!))
           (let [watch-key (keyword (gensym))]
             (add-watch
              rt/connections
              watch-key
              (fn [_ _ _old new]
                (when-let [send! (get new session-id)]
                  (@done)
                  (remove-watch rt/connections watch-key)
                  (resolve send!))))))))])))

(defn request
  ([message]
   (a/let [responses
           (.all js/Promise
                 (for [session-id (keys @rt/connections)]
                   (request session-id message)))]
     (last responses)))
  ([session-id message]
   (a/let [send! (get-connection session-id)]
     (if send!
       (let [id      (rt/next-id)
             message (assoc message :portal.rpc/id id)]
         (.then
          (js/Promise.
           (fn [resolve reject]
             (swap! rt/pending-requests assoc id [resolve reject])
             (send! message)))
          #(do (swap! rt/pending-requests dissoc id) %)))
       (throw (ex-info "No such portal session"
                       {:session-id session-id :message message}))))))

(defn- push-state [session-id new-value]
  (request session-id {:op :portal.rpc/push-state :state new-value})
  (rt/update-selected session-id [new-value])
  new-value)

(defrecord Portal [session-id]
  IDeref
  (-deref [_this] (first (get-in @rt/sessions [session-id :selected])))
  IReset
  (-reset! [_this new-value] (push-state session-id new-value))
  ISwap
  (-swap! [this f]
    (reset! this (f @this)))
  (-swap! [this f a]
    (reset! this (f @this a)))
  (-swap! [this f a b]
    (reset! this (f @this a b)))
  (-swap! [this f a b xs]
    (reset! this (apply f @this a b xs))))

(defn make-atom [session-id] (Portal. session-id))

(defn open? [session-id]
  (contains? @rt/connections session-id))
