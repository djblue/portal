(ns ^:no-doc portal.runtime.rpc
  (:require #?(:clj  [portal.runtime.jvm.client :as c]
               :cljs [portal.runtime.node.client :as c]
               :cljr [portal.runtime.clr.client :as c])
            [portal.runtime :as rt]))

(defn on-open [session send!]
  (swap! rt/connections
         assoc (:session-id session)
         (fn [message]
           (send! (rt/write message session))))
  (when-let [f (get-in session [:options :on-load])]
    (try
      (f)
      (catch #?(:cljs :default :default Exception) e
        (tap> e))))
  (when-let [f (get-in session [:options :on-load-1])]
    (try
      (f (c/make-atom (:session-id session)))
      (catch #?(:cljs :default :default Exception) e
        (tap> e)))))

(def ^:private ops (merge c/ops rt/ops))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn on-receive [session message]
  (let [send! (get @rt/connections (:session-id session))
        body  (rt/read message session)
        id    (:portal.rpc/id body)
        op    (get ops (:op body) not-found)
        done  (fn on-done [response]
                (send!
                 (assoc response
                        :portal.rpc/id id
                        :op :portal.rpc/response)))]
    (binding [rt/*session* session]
      (op body done))))

(defn on-close [session]
  (swap! rt/connections dissoc (:session-id session))
  (rt/reset-session session))