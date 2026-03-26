(ns portal.ssr.server
  (:refer-clojure :exclude [parse-uuid])
  (:require
   [clojure.data.json :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.server :as server]
   [portal.runtime :as rt]
   [portal.runtime.jvm.server :refer [enable-cors route]]
   [portal.shortcuts :as shortcuts]
   [portal.ssr.hiccup :as hiccup]
   [portal.ssr.ui.app :as app]
   [portal.ssr.ui.react :as react]
   [portal.ssr.ui.state :as state]
   [portal.ssr.ui.uuid :refer [parse-uuid]]
   [portal.ui.select :as select]
   [portal.ui.styled :as d])
  (:import [java.io ByteArrayInputStream]))

(defn- on-message [{:keys [handlers]} {:keys [id] :as event}]
  (let [op (keyword (:op event))]
    (if-let [f (get-in @handlers [(some-> id parse-uuid) op])]
      (f event)
      (if (= :on-key-down op)
        (shortcuts/keydown event)
        (tap> [:missing-handler event])))))

(defn- start-profile [session]
  (when (get-in session [:options :profile?])
    (try
      (let [start (requiring-resolve 'clj-async-profiler.core/start)]
        (start {:event :alloc}))
      (catch Exception e (tap> (Throwable->map e))))))

(defn- stop-profile [session]
  (when (get-in session [:options :profile?])
    (try
      (let [browse-url (requiring-resolve 'clojure.java.browse/browse-url)
            stop (requiring-resolve 'clj-async-profiler.core/stop)]
        (browse-url (stop)))
      (catch Exception e (tap> (Throwable->map e))))))

(defn- start-render-loop [session render]
  (let [running (atom true)]
    (future
      (start-profile session)
      (let [budget-time (/ 1000.0 30)]
        (loop [state nil]
          (when @running
            (recur
             (let [start (System/currentTimeMillis)]
               (try
                 (render state)
                 (catch Exception e
                   (reset! running false)
                   (tap> [state e]))
                 (finally
                   (let [total-time (- (System/currentTimeMillis) start)]
                     (when (< total-time budget-time)
                       (let [sleep-time (long (- budget-time total-time))]
                         (when (< sleep-time 10) (println sleep-time))
                         (Thread/sleep sleep-time))))))))))))
    (fn stop-render-loop []
      (reset! running false)
      (stop-profile session))))

(defonce ^:private render-loops (atom {}))

(defn- ->style [cache]
  (persistent!
   (reduce-kv
    (fn [out [selector style] class]
      (conj! out
             (str ((get d/selectors selector) class)
                  "{" (d/style->css style) "}" "\n")))
    (transient [])
    cache)))

(defn- process-event-queue! [session]
  (let [event-queue (atom nil)]
    (swap! (:event-queue session)
           (fn clear-event-queue [event-queue']
             (reset! event-queue event-queue')
             (empty event-queue')))
    (doseq [event @event-queue]
      (try
        (on-message session event)
        (catch Exception e (tap> e))))))

(defn send!
  ([message]
   (send! rt/*session* message))
  ([{:keys [channel]} message]
   (if (or (bytes? message)
           (instance? ByteArrayInputStream message))
     (server/send! channel message)
     (server/send! channel (cond-> message (not (string? message)) (json/write-str))))))

(defn- render-app [{:keys [handlers selection-index output-buffer] :as session}
                   {:keys [hiccup styles app-state] :as render-state}]
  (binding [rt/*session* session
            select/*selection-index* selection-index]
    (process-event-queue! session)
    (let [app-state' (some-> hiccup meta :state deref)]
      (if (and (some? app-state) (= app-state app-state'))
        render-state
        (let [cache (atom styles)
              hiccup'
              (if-not hiccup
                (react/render [app/app session])
                (react/render (meta hiccup) [app/app session]))]
          (when-not (= hiccup hiccup')
            (vreset! handlers {})
            (let [written-bytes
                  (binding [d/*cache* cache
                            hiccup/*handlers* handlers]
                    (hiccup/html! output-buffer hiccup'))]
              (when (not (identical? styles @cache))
                (send! session {:op "on-styles" :append-styles (->style (apply dissoc @cache (keys styles)))}))
              (send! session (hiccup/->input-stream output-buffer written-bytes))))
          {:hiccup hiccup' :styles @cache :app-state app-state'})))))

(defn- on-open [session]
  (add-watch (:state session) :selected #'state/send-selected-values)
  (swap! rt/connections assoc (:session-id session) (partial send! session))
  (swap! render-loops assoc (:session-id session)
         (start-render-loop session (partial #'render-app session))))

(defn- on-receive [session message]
  (swap! (:event-queue session) conj (json/read-str message :key-fn keyword)))

(defn- on-close [session]
  (remove-watch (:state session) :selected)
  (swap! rt/connections dissoc (:session-id session))
  (when-let [stop-render-loop (get @render-loops (:session-id session))]
    (stop-render-loop)
    (swap! render-loops dissoc (:session-id session))))

(defn- vs-code? [request]
  (some-> request
          (get-in [:headers "origin"])
          (str/starts-with? "vscode-webview://")))

(defn- open-session [{:keys [session] :as request}]
  (-> session
      (update :options
              (fn [options]
                (cond-> options
                  (not (contains? options :value))
                  (assoc :value @#'rt/tap-list)
                  (and (nil? (:theme options))
                       (vs-code? request))
                  (assoc :theme :portal.colors/vs-code-embedded))))
      (assoc :state (atom {})
             :handlers (volatile! {})
             :event-queue (atom [])
             :selection-index (atom {})
             :output-buffer (byte-array (* 5 1024 1024)))))

(defmethod route [:get "/ssr"] [request]
  (let [session (atom (open-session request))]
    (server/as-channel
     request
     {:on-receive (fn [_ch message]
                    (on-receive @session message))
      :on-open    (fn [ch]
                    (swap! session assoc :channel ch)
                    (on-open @session))
      :on-close   (fn [_ch _status]
                    (on-close @session))})))

(defmethod route [:options "/main.cljs"] [_] enable-cors)
(defmethod route [:get "/main.cljs"] [_]
  {:status  200
   :headers {"Access-Control-Allow-Origin" "*"}
   :body
   (slurp (io/resource "portal/ssr/ui/core.cljs"))})

(defn clear-values []
  (let [value (get-in rt/*session* [:options :value])]
    (when (instance? clojure.lang.Atom value)
      (swap! value empty))))

;; portal ui features
;; [x] finish porting inspector
;; [ ] enqueue state updates to avoid race conditions
;; [x] bind state/selection-index to web socket connection
;; [x] select values
;; [x] expand collapse values
;; [x] hover preview
;; [x] value filtering
;; [x] relative selection via keyboard
;; [x] pause / resume watching values
;; [x] path tracking
;; [x] commands / command palette
;; [x] shortcuts
;; [x] port more viewers
;; [x] fix component with multiple handlers of the same type
;; [ ] simplify viewer dispatch
;; [ ] figure out how to collapse ssr module back into existing code
;; [x] fix deref pause not working sometimes
;; [ ] fix :launcher :vs-code dependency on portal.runtime.index
;; [ ] profile / optimize rendeing perf
;; [ ] support portal apis for (deref, selected, ...) :ssr sessions