(ns portal.runtime.jvm.client
  (:import [clojure.lang IAtom IDeref]))

(defonce sessions (atom {}))

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

(defn- get-session [session-id]
  (let [p (promise)
        watch-key (keyword (gensym))]
    (if-let [send! (get @sessions session-id)]
      (deliver p send!)
      (add-watch
       sessions
       watch-key
       (fn [_ _ _old new]
         (when-let [send! (get new session-id)]
           (deliver p send!)))))
    (let [result (deref p timeout nil)]
      (remove-watch sessions watch-key)
      result)))

(defn request [session-id message]
  (if-let [send! (get-session session-id)]
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
                    {:session-id session-id :message message}))))

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
