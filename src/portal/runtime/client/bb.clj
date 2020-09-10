(ns portal.runtime.client.bb
  "Consolidate into jvm ns when bb gets support for implementing interfaces.")

(defonce sessions (atom {}))

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
       (future (done @(:request (get-session session))))))})

(defn request [session-id message]
  (let [{:keys [request response]} (get-session session-id)]
    (if-not (deliver request message)
      (throw (ex-info "Portal busy with another request" message))
      (let [response (deref response 1000 ::timeout)]
        (clear-session session-id)
        (if-not (= response ::timeout)
          response
          (throw (ex-info "Portal request timeout" message)))))))

(defn make-atom [_session-id])
