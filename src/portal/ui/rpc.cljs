(ns portal.ui.rpc
  (:refer-clojure :exclude [read type])
  (:require ["react" :as react]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.runtime.cson :as cson]
            [portal.ui.sci :as sci]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn call [f & args]
  (apply state/invoke f args))

(deftype RuntimeObject [object]
  cson/ToJson
  (-to-json [_]
    (cson/tag "ref" (:id object)))

  IMeta
  (-meta [_this] (:meta object))

  IWithMeta
  (-with-meta [_this m]
    (RuntimeObject.
     (assoc object :meta m)))

  IHash
  (-hash [_] (hash object))

  IPrintWithWriter
  (-pr-writer [_o writer _opts]
    (-write writer (str "#portal/runtime " (pr-str object)))))

(defn runtime-object? [value]
  (instance? RuntimeObject value))

(defn type [value] (:type (.-object value)))

(defn tag [value] (:tag (.-object value)))

(defn rep [value] (:rep (.-object value)))

(defn- -tag [tag value]
  (cson/tag tag (cson/to-json value)))

(extend-protocol cson/ToJson
  diff/Deletion
  (-to-json [this] (-tag "diff/Deletion" (:- this)))

  diff/Insertion
  (-to-json [this] (-tag "diff/Insertion" (:+ this)))

  diff/Mismatch
  (-to-json [this] (-tag "diff/Mismatch" ((juxt :- :+) this))))

(when (exists? js/BigInt)
  (extend-type js/BigInt
    IHash
    (-hash [this]
      (hash (.toString this)))))

(defn- diff-> [value]
  (case (first value)
    "diff/Deletion"  (diff/Deletion.  (cson/json-> (second value)))
    "diff/Insertion" (diff/Insertion. (cson/json-> (second value)))
    "diff/Mismatch"  (let [[a b] (cson/json-> (second value))]
                       (diff/Mismatch. a b))))

(defn- ref-> [value]
  (get @state/value-cache (second value)))

(defn- runtime-id [value]
  (or (-> value meta :portal.runtime/id)
      (when (runtime-object? value)
        (:id (.-object value)))))

(defn- read [string]
  (cson/read
   string
   {:transform
    (fn [value]
      (when-let [id (runtime-id value)]
        (swap! state/value-cache assoc id value))
      value)
    :default-handler
    (fn [value]
      (case (first value)
        "ref"    (ref-> value)
        "object" (let [object (cson/json-> (second value))]
                   (or
                    (get @state/value-cache (:id object))
                    (RuntimeObject. object)))
        (diff-> value)))}))

(defn- write [value]
  (cson/write
   value
   {:transform
    (fn [value]
      (if-let [id (-> value meta :portal.runtime/id)]
        (RuntimeObject. {:id id})
        value))}))

(defonce ^:private id (atom 0))
(defonce ^:private pending-requests (atom {}))

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
  (let [message (assoc message :portal.rpc/id (next-id))]
    ;; (tap> (assoc me ssage :type :request))
    ((if js/window.opener web-request ws-request) message)))

(defonce ^:private versions (r/atom {}))

(defn ^:no-doc use-invoke [f & args]
  (let [[value set-value!] (react/useState ::loading)
        versions           (for [arg args]
                             (str (hash arg) (get @versions arg)))]
    (react/useEffect
     (fn []
       (when (not-any? #{::loading} args)
         (-> (apply state/invoke f args)
             (.then #(set-value! %)))))
     (.from js/Array versions))
    value))

(def ^:private ops
  {:portal.rpc/response
   (fn [message _send!]
     (let [id        (:portal.rpc/id message)
           [resolve] (get @pending-requests id)]
       (swap! pending-requests dissoc id)
       (when (fn? resolve) (resolve message))))
   :portal.rpc/eval-str
   (fn [message send!]
     (send!
      (merge
       {:op            :portal.rpc/response
        :portal.rpc/id (:portal.rpc/id message)}
       (try
         {:result (sci/eval-string (:code message))}
         (catch :default e
           {:result (pr-str e)})))))
   :portal.rpc/update-versions
   (fn [message send!]
     (reset! versions (:body message))
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
     (state/dispatch! state/state state/clear)
     (reset! versions {})
     (send! {:op :portal.rpc/response
             :portal.rpc/id (:portal.rpc/id message)}))
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

(defn- connect []
  (if-let [ws @ws-promise]
    ws
    (reset!
     ws-promise
     (js/Promise.
      (fn [resolve reject]
        (when-let [chan (js/WebSocket.
                         (str "ws://" (get-host) "/rpc?" (get-session)))]
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
