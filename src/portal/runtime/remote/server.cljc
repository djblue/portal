(ns ^:no-doc portal.runtime.remote.server
  (:require [portal.runtime :as rt]
            #?(:clj  [portal.runtime.jvm.client :as c]
               :cljs [portal.runtime.node.client :as c])))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn open [session-id]
  (let [queue   (atom [])
        session {:responses   queue
                 :value-cache (atom {})
                 :session-id  session-id}
        send!   (fn send! [message]
                  (swap! queue conj message))
        session (assoc session :send! send!)]
    (swap! rt/sessions assoc session-id session)
    (swap! c/connections assoc session-id send!)
    nil))

(defn responses [session-id]
  (let [session   (get @rt/sessions session-id)
        responses (:responses session)]
    (doseq [message @responses]
      (println (rt/write message session)))
    (reset! responses [])
    nil))

(defn close [session-id]
  (swap! rt/sessions dissoc session-id)
  (swap! c/connections dissoc session-id)
  nil)

(def ^:private ops (merge rt/ops))

(defn request [session-id message]
  (let [session (get @rt/sessions session-id)
        body    (rt/read message session)
        id      (:portal.rpc/id body)
        op      (get ops (:op body) not-found)]
    (binding [rt/*session* session]
      (op body #((:send! session)
                 (assoc %
                        :portal.rpc/id id
                        :op :portal.rpc/response)))))
  nil)

(reset! rt/request c/request)
(add-tap #'rt/update-value)
