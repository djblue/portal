(ns portal.ui.rpc
  (:require [cognitect.transit :as t]
            [com.cognitect.transit.types :as ty]
            [portal.ui.viewer.diff :as diff]
            [portal.ui.app :refer [get-datafy]]
            [portal.ui.state :refer [state tap-state notify-parent]]))

;; Since any object can have metadata and all unknown objects in portal
;; are encoded as tagged values, if any of those objects have metadata, it
;; will be problematic. Tagged values are represented in transit with
;; ty/TaggedValue which doesn't support metadata, leading to error when
;; reading. Implementing the metadata protocols will fix the issue for
;; now. The crux of the problem is that write-meta gets to the metadata
;; before it can be hidden in the representation.
(extend-type ty/TaggedValue
  IMeta
  (-meta [this]
    (:meta (.-rep this)))

  IWithMeta
  (-with-meta [this m]
    (t/tagged-value
     (.-tag this)
     (assoc (.-rep this) :meta m))))

(defn- json->edn [json]
  (let [r (t/reader :json {:handlers diff/readers})]
    (t/read r json)))

(defn- edn->json [edn]
  (let [w (t/writer
           :json
           {:transform t/write-meta
            :handlers diff/writers})]
    (t/write w edn)))

(defonce ^:private id (atom 0))
(defonce ^:private pending-requests (atom {}))

(defn- next-id [] (swap! id inc))

(declare send!)

(defn- ws-request [message]
  (js/Promise.
   (fn [resolve reject]
     (let [id (next-id)]
       (swap! pending-requests assoc id [resolve reject])
       (send! (assoc message :portal.rpc/id id))))))

(defn- web-request [message]
  (-> (edn->json message)
      js/window.opener.portal.web.send_BANG_
      (.then json->edn)))

(def request (if js/window.opener web-request ws-request))

(def ^:private ops
  {:portal.rpc/response
   (fn [message _send!]
     (let [id        (:portal.rpc/id message)
           [resolve] (get @pending-requests id)]
       (swap! pending-requests dissoc id)
       (when (fn? resolve) (resolve message))))
   :portal.rpc/datafy
   (fn [message send!]
     (let [value  (:portal/value (merge @tap-state @state))
           datafy (get-datafy @state)]
       (send!
        {:op :portal.rpc/response
         :portal.rpc/id (:portal.rpc/id message)
         :portal/value (datafy value)})))
   :portal.rpc/close
   (fn [message send!]
     (js/setTimeout
      (fn []
        (notify-parent {:type :close})
        (js/window.close))
      100)
     (send! {:op :portal.rpc/response
             :portal.rpc/id (:portal.rpc/id message)}))
   :portal.rpc/push-state
   (fn [message send!]
     (swap! state
            (fn [state]
              (assoc state
                     :portal/previous-state state
                     :portal/next-state nil
                     :search-text ""
                     :portal/value (:state message))))
     (send!
      {:op :portal.rpc/response
       :portal.rpc/id (:portal.rpc/id message)}))})

(defn- dispatch [message send!]
  (when-let [f (get ops (:op message))] (f message send!)))

(defn ^:export handler [request]
  (edn->json (dispatch (json->edn request) identity)))

(defonce ^:private ws-promise (atom nil))

(defn- get-session [] (subs js/window.location.search 1))

(defn- connect []
  (if-let [ws @ws-promise]
    ws
    (reset!
     ws-promise
     (js/Promise.
      (fn [resolve]
        (when-let [chan (js/WebSocket.
                         (str "ws://" js/location.host "/rpc?" (get-session)))]
          (set! (.-onmessage chan) #(dispatch (json->edn (.-data %))
                                              (fn [message]
                                                (send! message))))
          (set! (.-onerror chan)   #(reset!  ws-promise nil))
          (set! (.-onclose chan)   #(reset!  ws-promise nil))
          (set! (.-onopen chan)    #(resolve chan))))))))

(defn send! [message]
  (.then (connect) #(.send % (edn->json message))))
