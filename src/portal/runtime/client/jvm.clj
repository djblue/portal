(ns portal.runtime.client.jvm
  (:require [portal.runtime.client.bb :as bb])
  (:import [clojure.lang IAtom IDeref]))

(def ops bb/ops)
(def request bb/request)

(defn- push-state [session-id new-value]
  (request session-id {:op :portal.rpc/push-state :state new-value})
  new-value)

(defn- datafy [session-id]
  (:portal/value (request session-id {:op :portal.rpc/datafy})))

(defrecord Portal [session-id]
  IDeref
  (deref [this] (datafy session-id))

  IAtom
  (reset [this new-value] (push-state session-id new-value))

  (swap  [this f]          (reset! this (f @this)))
  (swap  [this f a]        (reset! this (f @this a)))
  (swap  [this f a b]      (reset! this (f @this a b)))
  (swap  [this f a b args] (reset! this (apply f @this a b args)))
  (compareAndSet [this oldv newv]))

(defmethod print-method Portal [portal w]
  (print-method (into {} portal) w))

(defn make-atom [session-id] (Portal. session-id))
