(ns ^:no-doc portal.runtime.web.client
  (:require [portal.runtime :as rt]))

(defonce session {:id (atom 0)
                  :session-id ::id
                  :value-cache (atom {})})

(defn request [session-handle message]
  (if-let [child-window @session-handle]
    (rt/read
     (.portal.ui.rpc.handler ^js child-window (rt/write message session))
     session)
    (throw (ex-info "Portal not open" message))))

(defn- push-state [session new-value]
  (request session {:op :portal.rpc/push-state :state new-value})
  (rt/update-selected ::id new-value)
  new-value)

(deftype Portal [session]
  IDeref
  (-deref [_this] (get-in @rt/sessions [::id :selected]))
  IReset
  (-reset! [_this new-value] (push-state session new-value))
  ISwap
  (-swap! [this f]
    (reset! this (f @this)))
  (-swap! [this f a]
    (reset! this (f @this a)))
  (-swap! [this f a b]
    (reset! this (f @this a b)))
  (-swap! [this f a b xs]
    (reset! this (apply f @this a b xs))))

(defn make-atom [session] (Portal. session))

