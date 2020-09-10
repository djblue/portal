(ns portal.ui.rpc
  (:require [cognitect.transit :as t]
            [portal.ui.app :refer [get-datafy]]
            [portal.ui.state :refer [state tap-state]]
            [com.cognitect.transit.types :as ty]))

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

(defn json->edn [json]
  (let [r (t/reader :json)] (t/read r json)))

(defn edn->json [edn]
  (let [w (t/writer :json {:transform t/write-meta})]
    (t/write w edn)))

(defn- get-mode [] (if js/window.opener ::web ::server))

(defn- get-sender []
  (case (get-mode)
    ::web js/window.opener.portal.web.send_BANG_
    ::server
    (fn server-rpc [body]
      (-> (js/fetch "/rpc" #js {:method "POST" :body body})
          (.then #(.text %))))))

(defonce sender (get-sender))

(defn send! [msg]
  (-> (sender (edn->json msg))
      (.then json->edn)))

(defn get-session [] (uuid (subs js/window.location.search 1)))

(defonce session-id (get-session))

(def ops
  {:portal.rpc/datafy
   (fn [_request]
     (let [value  (:portal/value (merge @tap-state @state))
           datafy (get-datafy @state)]
       {:portal/value (datafy value)}))
   :portal.rpc/push-state
   (fn [request]
     (swap! state
            (fn [state]
              (assoc state
                     :portal/previous-state state
                     :portal/next-state nil
                     :search-text ""
                     :portal/value (:state request))))
     nil)})

(defn- dispatch [request]
  (when-let [f (get ops (:op request))] (f request)))

(defn ^:export handler [request]
  (edn->json (dispatch (json->edn request))))

(defn recv! []
  (-> (send!
       {:op :portal.rpc/recv-request
        :portal.rpc/session-id session-id})
      (.then dispatch)
      (.then
       (fn [response]
         (send!
          {:op :portal.rpc/send-response
           :portal.rpc/session-id session-id
           :response response})))))

(defn long-poll []
  (when (= (get-mode) ::server)
    (.then (recv!) long-poll)))

(defonce init (long-poll))
