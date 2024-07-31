(ns ^:no-doc portal.runtime.clr.client
  (:require [clojure.pprint :as pprint]
            [portal.runtime :as rt])
  (:import [clojure.lang IAtom IDeref]))

(def ops
  {:portal.rpc/response
   (fn [message _send!]
     (let [id (:portal.rpc/id message)]
       (when-let [response (get @rt/pending-requests id)]
         (deliver response message))))})

(def timeout 60000)

(defn- get-connection [session-id]
  (let [p (promise)
        watch-key (keyword (gensym))]
    (if-let [send! (get @rt/connections session-id)]
      (deliver p send!)
      (add-watch
       rt/connections
       watch-key
       (fn [_ _ _old new]
         (when-let [send! (get new session-id)]
           (deliver p send!)))))
    (let [result (deref p timeout nil)]
      (remove-watch rt/connections watch-key)
      result)))

(defn- request! [session-id message]
  (if-let [send! (get-connection session-id)]
    (let [id       (rt/next-id)
          response (promise)
          message  (assoc message :portal.rpc/id id)]
      (swap! rt/pending-requests assoc id response)
      (send! message)
      (let [response (deref response timeout ::timeout)]
        (swap! rt/pending-requests dissoc id)
        (if-not (= response ::timeout)
          response
          (throw (ex-info "Portal request timeout"
                          {:session-id session-id :message message})))))
    (throw (ex-info "No such portal session"
                    {:session-id session-id :message message}))))

(defn- broadcast! [message]
  (when-let [sessions (keys @rt/connections)]
    (let [response (promise)]
      (doseq [session-id sessions]
        (future
          (try
            (deliver response (request! session-id message))
            (catch Exception ex
              (when (-> ex ex-data ::timeout)
                (swap! rt/connections dissoc session-id))
              (deliver response ex)))))
      (let [response (deref response timeout ::timeout)]
        (cond
          (instance? Exception response)
          (throw response)
          (not= response ::timeout)
          response
          :else
          (throw (ex-info
                  "Portal request timeout"
                  {::timeout true
                   :session-id :all
                   :message message})))))))

(defn request
  ([message]
   (broadcast! message))
  ([session-id message]
   (request! session-id message)))

(defn- push-state [session-id new-value]
  (request session-id {:op :portal.rpc/push-state :state new-value})
  (rt/update-selected session-id [new-value])
  new-value)

(defrecord Portal [session-id]
  IDeref
  (deref [_this] (first (get-in @rt/sessions [session-id :selected])))

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
  (contains? @rt/connections session-id))
