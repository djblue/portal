(ns portal.runtime.remote.server
  (:require [portal.runtime :as rt]
            #?(:clj  [portal.runtime.jvm.client :as c]
               :cljs [portal.runtime.node.client :as c])))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn open [session-id]
  (let [queue   (atom [])
        options {:responses   queue
                 :value-cache (atom {})
                 :session-id  session-id}
        send!   (fn send! [message]
                  (swap! queue conj message))
        session (assoc options :send! send!)]
    (swap! rt/sessions assoc session-id session)
    (swap! c/sessions assoc session-id send!)
    nil))

(defn responses [session-id]
  (let [options   (get @rt/sessions session-id)
        responses (:responses options)]
    (doseq [message @responses]
      (println (rt/write message options)))
    (reset! responses [])
    nil))

(defn close [session-id]
  (swap! rt/sessions dissoc session-id)
  (swap! c/sessions dissoc session-id)
  nil)

(def ^:private ops (merge rt/ops))

(defn request [session-id message]
  (let [options (get @rt/sessions session-id)
        body    (rt/read message options)
        id      (:portal.rpc/id body)
        op      (get ops (:op body) not-found)]
    (binding [rt/*options* options]
      (op body #((:send! options)
                 (assoc %
                        :portal.rpc/id id
                        :op :portal.rpc/response)))))
  nil)

(reset! rt/request c/request)
(add-tap #'rt/update-value)
