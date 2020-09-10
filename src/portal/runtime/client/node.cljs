(ns portal.runtime.client.node)

(defonce sessions (atom {}))

(defn- promise []
  (let [p (atom {})]
    (swap!
     p
     assoc
     :promise
     (js/Promise.
      (fn [resolve reject]
        (swap! p
               assoc
               :resolve resolve
               :reject reject))))
    p))

(defn- deliver [p value]
  (when-let [resolve (:resolve @p)]
    (swap! p dissoc :resolve)
    (resolve p value)))

(defn- then [p f]
  (when-let [p (:promise @p)] (.then p f)))

(defn- get-session [session-id]
  (get
   (swap! sessions
          update
          session-id
          #(or % {:request (promise) :response (promise)}))
   session-id))

(defn- clear-session [session-id]
  (swap! sessions dissoc session-id))

(def ops
  {:portal.rpc/send-response
   (fn [request done]
     (let [session-id (:portal.rpc/session-id request)]
       (when-let [response (:response (get-session session-id))]
         (clear-session session-id)
         (deliver response (:response request))))
     (done {}))

   :portal.rpc/recv-request
   (fn [request done]
     (let [session (:portal.rpc/session-id request)]
       (then (:request (get-session session)) done)))})

(defn request [session-id message]
  (let [{:keys [request response]} (get-session session-id)]
    (if-not (deliver request message)
      (throw (ex-info "Portal busy with another request" message))
      ;; TODO: add race / timeout
      (:promise @response))))

(defn make-atom [_session-id])
