(ns ^:no-doc portal.runtime.web.client
  (:require [portal.runtime :as rt]))

(defonce connection (atom nil))

(defonce session (atom {:session-id ::id}))

(defn request
  ([message]
   (request ::id message))
  ([_session-id message]
   (if-let [child-window @connection]
     (rt/read
      (.portal.ui.rpc.handler ^js child-window (rt/write message @session))
      @session)
     (throw (ex-info "Portal not open" message)))))

(defn- push-state [session-id new-value]
  (request session-id {:op :portal.rpc/push-state :state new-value})
  (rt/update-selected session-id [new-value])
  new-value)

(deftype Portal [session-id]
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

(defn sessions [] (when @connection (list (make-atom ::id))))
