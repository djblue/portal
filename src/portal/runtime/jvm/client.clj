(ns ^:no-doc portal.runtime.jvm.client
  (:require [clojure.pprint :as pprint]
            [portal.runtime :as rt])
  (:import [clojure.lang IAtom IDeref]))

(defonce connections (atom {}))

(defonce ^:private id (atom 0))
(defonce ^:private pending-requests (atom {}))

(defn- next-id [] (swap! id inc))

(def ops
  {:portal.rpc/response
   (fn [message _send!]
     (let [id (:portal.rpc/id message)]
       (when-let [response (get @pending-requests id)]
         (deliver response message))))})

(def timeout 10000)

(defn- get-connection [session-id]
  (let [p (promise)
        watch-key (keyword (gensym))]
    (if-let [send! (get @connections session-id)]
      (deliver p send!)
      (add-watch
       connections
       watch-key
       (fn [_ _ _old new]
         (when-let [send! (get new session-id)]
           (deliver p send!)))))
    (let [result (deref p timeout nil)]
      (remove-watch connections watch-key)
      result)))

(defn request
  ([message]
   (last
    (for [session-id (keys @connections)]
      (request session-id message))))
  ([session-id message]
   (if-let [send! (get-connection session-id)]
     (let [id       (next-id)
           response (promise)
           message  (assoc message :portal.rpc/id id)]
       (swap! pending-requests assoc id response)
       (send! message)
       (let [response (deref response timeout ::timeout)]
         (swap! pending-requests dissoc id)
         (if-not (= response ::timeout)
           response
           (throw (ex-info "Portal request timeout"
                           {:session-id session-id :message message})))))
     (throw (ex-info "No such portal session"
                     {:session-id session-id :message message})))))

(defn- push-state [session-id new-value]
  (request session-id {:op :portal.rpc/push-state :state new-value})
  (rt/update-selected session-id new-value)
  new-value)

(defrecord Portal [session-id]
  IDeref
  (deref [_this] (get-in @rt/sessions [session-id :selected]))

  IAtom
  (reset [_this new-value] (push-state session-id new-value))

  (swap  [this f]          (reset! this (f @this)))
  (swap  [this f a]        (reset! this (f @this a)))
  (swap  [this f a b]      (reset! this (f @this a b)))
  (swap  [this f a b args] (reset! this (apply f @this a b args)))
  (compareAndSet [_this _oldv _newv]))

(defmethod print-method Portal [portal w]
  (print-method (into {} portal) w))

(defmethod pprint/simple-dispatch Portal [portal] (pr portal))

(defn make-atom [session-id] (Portal. session-id))

(defn open? [session-id]
  (contains? @connections session-id))
