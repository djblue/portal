(ns portal.runtime.node.client)

(defonce sessions (atom {}))

(defonce ^:private id (atom 0))
(defonce ^:private pending-requests (atom {}))

(defn- next-id [] (swap! id inc))

(defn- get-session [session-id]
  (get @sessions session-id))

(def ops
  {:portal.rpc/response
   (fn [message _done]
     (let [id (:portal.rpc/id message)]
       (when-let [[resolve] (get @pending-requests id)]
         (resolve message))))})

(defn request [session-id message]
  (if-let [send! (get-session session-id)]
    (let [id      (next-id)
          message (assoc message :portal.rpc/id id)]
      (.then
       (js/Promise.
        (fn [resolve reject]
          (swap! pending-requests assoc id [resolve reject])
          (send! message)))
       #(do (swap! pending-requests dissoc id) %)))
    (throw (ex-info "No such portal session"
                    {:session-id session-id :message message}))))

(defn make-atom [_session-id])
