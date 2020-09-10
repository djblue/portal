(ns portal.runtime.client.web
  (:require [portal.runtime.transit :as t]))

(defn request [session message]
  (if-let [child-window @session]
    (t/json->edn
     (.portal.ui.rpc.handler child-window (t/edn->json message)))
    (throw (ex-info "Portal not open" message))))

(defn- push-state [session new-value]
  (request session {:op :portal.rpc/push-state :state new-value})
  new-value)

(defn- datafy [session]
  (:portal/value (request session {:op :portal.rpc/datafy})))

(deftype Portal [session]
  IDeref
  (-deref [this] (datafy session))
  IReset
  (-reset! [this new-value] (push-state session new-value))
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

