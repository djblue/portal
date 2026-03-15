(ns portal.ssr.ui.rpc
  (:require #?(:clj  [portal.runtime.jvm.client :as c]
               :cljs [portal.runtime.node.client :as c]
               :cljr [portal.runtime.clr.client :as c]
               :lpy  [portal.runtime.python.client :as c])
            [portal.runtime :as rt]
            [portal.runtime.protocols :as p]))

(defn on-open [session send!]
  (swap! rt/connections assoc (:session-id session) send!)
  (when-let [on-open (-> session :options :on-open)]
    (on-open session)))

(defn on-receive [session message]
  (binding [rt/*session* session]
    (when-let [on-message (-> session :options :on-message)]
      (on-message message))))

(defn on-close [session]
  (swap! rt/connections dissoc (:session-id session))
  (when-let [on-close (-> session :options :on-close)]
    (on-close session)))

(defn listener [session]
  (reify p/Listener
    (on-open    [_ socket]
      (on-open session (fn send [message]
                         (p/send socket message))))
    (on-message [_ _socket message]
      (on-receive session message))
    (on-close   [_ _socket _code _reason]
      (on-close session))
    (on-pong    [_ _socket _data])
    (on-error   [_ _socket _throwable])))