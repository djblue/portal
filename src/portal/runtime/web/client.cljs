(ns portal.runtime.web.client
  (:require [portal.runtime :as rt]))

(defn request [session message]
  (if-let [child-window @session]
    (rt/read
     (.portal.ui.rpc.handler ^js child-window (rt/write message)))
    (throw (ex-info "Portal not open" message))))

(defn- push-state [session new-value]
  (request session {:op :portal.rpc/push-state :state new-value})
  new-value)

(defn- datafy [session]
  (:portal/value (request session {:op :portal.rpc/datafy})))

(deftype Portal [session]
  IDeref
  (-deref [_this] (datafy session))
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

