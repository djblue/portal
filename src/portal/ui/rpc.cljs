(ns portal.ui.rpc
  (:refer-clojure :exclude [read])
  (:require [portal.runtime.cson :as cson]
            [portal.ui.state :as state]))

(deftype RuntimeObject [rep]
  cson/ToJson
  (-to-json [_]
    #js ["ref" (:id rep)])

  IWithMeta
  (-with-meta [_this m]
    (RuntimeObject.
     (assoc rep :meta m)))

  IPrintWithWriter
  (-pr-writer [_ writer _]
    (write-all writer (:pr-str rep))))

(defn read [string]
  (cson/read
   string
   (fn [value]
     (case (first value)
       "object" (RuntimeObject.
                 (cson/json-> (second value)))))))

(defn write [value] (cson/write value))

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
