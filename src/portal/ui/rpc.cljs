(ns portal.ui.rpc
  (:refer-clojure :exclude [read])
  (:require [cognitect.transit :as t]
            [com.cognitect.transit.types :as ty]
            [portal.runtime.cson :as cson]
            [portal.ui.state :as state]))

(defn read [string]
  (cson/read
   string
   (fn [value]
     (case (first value)
       "object" (t/tagged-value
                 "portal.transit/object"
                 (cson/json-> (second value)))))))

(defn write [value] (cson/write value))

;; Since any object can have metadata and all unknown objects in portal
;; are encoded as tagged values, if any of those objects have metadata, it
;; will be problematic. Tagged values are represented in transit with
;; ty/TaggedValue which doesn't support metadata, leading to error when
;; reading. Implementing the metadata protocols will fix the issue for
;; now. The crux of the problem is that write-meta gets to the metadata
;; before it can be hidden in the representation.
(extend-type ty/TaggedValue
  cson/ToJson
  (-to-json [this]
    #js ["object" (cson/to-json (.-rep this))])

  IMeta
  (-meta [this]
    (:meta (.-rep this)))

  IWithMeta
  (-with-meta [this m]
    (t/tagged-value
     (.-tag this)
     (assoc (.-rep this) :meta m)))

  IPrintWithWriter
  (-pr-writer [this writer _]
    (let [tag (.-tag this)
          rep (.-rep this)]
      (write-all
       writer
       (case tag
         "portal.transit/var"
         (str "#'" rep)

         "portal.transit/object"
         (:string rep)

         "r" rep

         "ratio"
         (let [[a b] rep]
           (str (.-rep a) "/" (.-rep b)))

         (str "#" tag rep))))))

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
  (js/Promise.
   (fn [resolve reject]
     (try
       (-> (write message)
           js/window.opener.portal.web.send_BANG_
           (.then read)
           resolve)
       (catch :default e (reject e))))))

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
     (let [value (state/get-selected-value @state/state)]
       (send!
        {:op :portal.rpc/response
         :portal.rpc/id (:portal.rpc/id message)
         :portal/value value})))
   :portal.rpc/close
   (fn [message send!]
     (js/setTimeout
      (fn []
        (state/notify-parent {:type :close})
        (js/window.close))
      100)
     (send! {:op :portal.rpc/response
             :portal.rpc/id (:portal.rpc/id message)}))
   :portal.rpc/clear
   (fn [message send!]
     (state/dispatch! state/state state/clear)
     (send! {:op :portal.rpc/response
             :portal.rpc/id (:portal.rpc/id message)}))
   :portal.rpc/push-state
   (fn [message send!]
     (state/dispatch! state/state state/history-push {:portal/value (:state message)})
     (send!
      {:op :portal.rpc/response
       :portal.rpc/id (:portal.rpc/id message)}))})

(defn- dispatch [message send!]
  (when-let [f (get ops (:op message))] (f message send!)))

(defn ^:export handler [request]
  (write (dispatch (read request) identity)))

(defonce ^:private ws-promise (atom nil))

(defn- get-session [] (subs js/window.location.search 1))

(defn- connect []
  (if-let [ws @ws-promise]
    ws
    (reset!
     ws-promise
     (js/Promise.
      (fn [resolve reject]
        (when-let [chan (js/WebSocket.
                         (str "ws://" js/location.host "/rpc?" (get-session)))]
          (set! (.-onmessage chan) #(dispatch (read (.-data %))
                                              (fn [message]
                                                (send! message))))
          (set! (.-onerror chan)   (fn [e]
                                     (reject e)
                                     (doseq [[_ [_ reject]] @pending-requests]
                                       (reject e))))
          (set! (.-onclose chan)   #(reset!  ws-promise nil))
          (set! (.-onopen chan)    #(resolve chan))))))))

(defn send! [message]
  (.then (connect) #(.send % (write message))))
