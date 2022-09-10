(ns portal.ui.rpc
  (:refer-clojure :exclude [read type])
  (:require [portal.async :as a]
            [portal.runtime.cson :as cson]
            [portal.ui.rpc.runtime :as rt]
            [portal.ui.sci :as sci]
            [portal.ui.state :as state]
            [portal.ui.viewer.diff :as diff])
  (:import [goog.math Long]))

(defn call [f & args]
  (apply state/invoke f args))

(when (exists? js/BigInt)
  (extend-type js/BigInt
    IHash
    (-hash [this]
      (hash (.toString this)))
    IPrintWithWriter
    (-pr-writer [this writer _opts]
      (-write writer (str this "N")))))

(extend-type Long
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (-write writer (str this))))

(extend-type default
  cson/ToJson
  (-to-json [value buffer]
    (cson/tag buffer "remote" (pr-str value))))

(defn- read [string]
  (cson/read
   string
   {:transform rt/transform
    :default-handler
    (fn [op value]
      (case op
        "ref"    (rt/->value value)
        "object" (rt/->object call value)
        "remote" (cson/tagged-value "remote" value)
        (diff/->diff op value)))}))

(defn- write [value]
  (cson/write
   value
   {:transform
    (fn [value]
      (if-let [id (-> value meta :portal.runtime/id)]
        (cson/tagged-value "ref" id)
        value))}))

(defonce ^:private id (atom 0))
(defonce ^:private pending-requests (atom {}))

(defonce log
  (atom
   (with-meta
     (list)
     {:portal.viewer/default :portal.viewer/table})))

(defn- next-id [] (swap! id inc))

(declare send!)

(defn- ws-request [message]
  (js/Promise.
   (fn [resolve reject]
     (let [id (:portal.rpc/id message)]
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

(defn request [message]
  (a/let [message  (assoc message :portal.rpc/id (next-id))
          start    (.now js/Date)
          response ((if js/window.opener web-request ws-request) message)]
    (when (and js/goog.DEBUG
               (not= 'portal.runtime/ping (:f message)))
      (swap! log
             (fn [log]
               (with-meta
                 (take 10
                       (conj log
                             (-> message
                                 (dissoc :op :portal.rpc/id)
                                 (assoc :return  (:return response)
                                        :time-ms (- (.now js/Date) start)))))
                 (meta log)))))
    response))

(def ^:private ops
  {:portal.rpc/response
   (fn [message _send!]
     (let [id        (:portal.rpc/id message)
           [resolve] (get @pending-requests id)]
       (swap! pending-requests dissoc id)
       (when (fn? resolve) (resolve message))))
   :portal.rpc/eval-str
   (fn [message send!]
     (try
       (send!
        {:op            :portal.rpc/response
         :result        (sci/eval-string (:code message))
         :portal.rpc/id (:portal.rpc/id message)})
       (catch :default e
         (.error js/console e)
         (send!
          {:op            :portal.rpc/response
           :error         true
           :message       (.-message e)
           :result        e
           :portal.rpc/id (:portal.rpc/id message)}))))
   :portal.rpc/invalidate
   (fn [message send!]
     (rt/deref (:atom message))
     (send! {:op :portal.rpc/response
             :portal.rpc/id (:portal.rpc/id message)}))
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
     (a/do
       (state/dispatch! state/state state/clear)
       (reset! rt/current-values {})
       (send! {:op :portal.rpc/response
               :portal.rpc/id (:portal.rpc/id message)})))
   :portal.rpc/push-state
   (fn [message send!]
     (state/dispatch! state/state state/history-push {:portal/value (:state message)})
     (send!
      {:op :portal.rpc/response
       :portal.rpc/id (:portal.rpc/id message)}))})

(defn- dispatch [message send!]
  ;; (tap> (assoc message :type :response))
  (when-let [f (get ops (:op message))] (f message send!)))

(defn ^:export ^:no-doc handler [request]
  (write (dispatch (read request) identity)))

(defonce ^:private ws-promise (atom nil))

(defn- get-session []
  (if (exists? js/PORTAL_SESSION)
    js/PORTAL_SESSION
    (subs js/window.location.search 1)))

(defn- get-host []
  (if (exists? js/PORTAL_HOST) js/PORTAL_HOST js/location.host))

(defn- get-proto []
  (if (= (.-protocol js/location) "https:") "wss:" "ws:"))

(defn- connect []
  (if-let [ws @ws-promise]
    ws
    (reset!
     ws-promise
     (js/Promise.
      (fn [resolve reject]
        (when-let [chan (js/WebSocket.
                         (str (get-proto) "//" (get-host) "/rpc?" (get-session)))]
          (set! (.-onmessage chan) #(dispatch (read (.-data %))
                                              (fn [message]
                                                (send! message))))
          (set! (.-onerror chan)   (fn [e]
                                     (reject e)
                                     (doseq [[_ [_ reject]] @pending-requests]
                                       (reject e))))
          (set! (.-onclose chan)   #(reset!  ws-promise nil))
          (set! (.-onopen chan)    #(resolve chan))))))))

(defn- send! [message]
  (.then (connect) #(.send % (write message))))
