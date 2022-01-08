(ns portal.ui.rpc
  (:refer-clojure :exclude [read type])
  (:require ["react" :as react]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.async :as a]
            [portal.runtime.cson :as cson]
            [portal.ui.sci :as sci]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn call [f & args]
  (apply state/invoke f args))

(defonce ^:private current-values (r/atom {}))

(defn- remote-deref [this]
  (-> (call 'clojure.core/deref this)
      (.then #(swap! current-values
                     assoc-in [this 'clojure.core/deref] %))))

(defn- remote-pr-str [this]
  (-> (call 'clojure.core/pr-str this)
      (.then #(swap! current-values
                     assoc-in [this 'clojure.core/pr-str] %))))

(defn- runtime-deref [this]
  (when (= ::not-found (get-in @current-values [this 'clojure.core/deref] ::not-found))
    (remote-deref this))
  (get-in @current-values [this 'clojure.core/deref]))

(defn- runtime-print [this writer _opts]
  (when (= ::not-found (get-in @current-values [this 'clojure.core/pr-str] ::not-found))
    (remote-pr-str this))
  (-write writer (get-in @current-values [this 'clojure.core/pr-str] "loading")))

(defn- runtime-to-json [this]
  (let [object (.-object this)]
    (if-let [to-object (:to-object cson/*options*)]
      (to-object this :runtime-object nil)
      (cson/tag "ref" (:id object)))))

(defn- runtime-meta [this] (:meta (.-object this)))

(defprotocol Runtime)

(declare ->runtime)

(deftype RuntimeObject [object]
  Runtime
  cson/ToJson (-to-json [this] (runtime-to-json this))
  IMeta       (-meta    [this] (runtime-meta this))
  IHash       (-hash    [_]    (hash object))
  IWithMeta
  (-with-meta [_this m]
    (RuntimeObject.
     (assoc object :meta m)))
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (runtime-print this writer _opts)))

(deftype RuntimeAtom [object]
  Runtime
  cson/ToJson (-to-json [this] (runtime-to-json this))
  IMeta       (-meta    [this] (runtime-meta this))
  IDeref      (-deref   [this] (runtime-deref this))
  IHash       (-hash    [_]    (hash object))
  IWithMeta
  (-with-meta [_this m]
    (RuntimeAtom.
     (assoc object :meta m)))
  IPrintWithWriter
  (-pr-writer [this writer _opts]
    (runtime-print this writer _opts)))

(defn runtime? [value]
  (satisfies? Runtime value))

(defn- ->runtime [object]
  (if (contains? (:protocols object) :IDeref)
    (->RuntimeAtom object)
    (->RuntimeObject object)))

(defn tag [value] (:tag (.-object value)))

(defn rep [value] (:rep (.-object value)))

(defn- to-json [tag value]
  (cson/tag tag (cson/to-json value)))

(extend-protocol cson/ToJson
  diff/Deletion
  (-to-json [this] (to-json "diff/Deletion" (:- this)))

  diff/Insertion
  (-to-json [this] (to-json "diff/Insertion" (:+ this)))

  diff/Mismatch
  (-to-json [this] (to-json "diff/Mismatch" ((juxt :- :+) this))))

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
      (when (runtime? value)
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
                    (->runtime object)))
        (diff-> value)))}))

(defn- write [value]
  (cson/write
   value
   {:transform
    (fn [value]
      (if-let [id (-> value meta :portal.runtime/id)]
        (->runtime {:id id})
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

(defn ^:no-doc use-invoke [f & args]
  (let [[value set-value!] (react/useState ::loading)
        versions           (for [arg args] (hash arg))]
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
   :portal.rpc/invalidate
   (fn [message send!]
     (remote-deref (:atom message))
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
     (reset! current-values {})
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
