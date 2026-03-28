(ns ^:no-doc portal.runtime.jvm.ssr
  (:refer-clojure :exclude [parse-uuid])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.server :as server]
   [portal.runtime :as rt]
   [portal.runtime.fs :as fs]
   [portal.runtime.json :as json]
   [portal.runtime.jvm.hiccup :as hiccup]
   [portal.runtime.jvm.server :refer [enable-cors route]]
   [portal.runtime.polyfill :refer [parse-uuid]]
   [portal.runtime.react :as react]
   [portal.shortcuts :as shortcuts]
   [portal.ui.app :as app]
   [portal.ui.select :as select]
   [portal.ui.state :as state]
   [portal.ui.styled :as d])
  (:import
   [java.io ByteArrayInputStream]
   [java.net URI]))

(defn- on-message [{:keys [handlers last-ping]} {:keys [id] :as event}]
  (let [op (keyword (:op event))]
    (case op
      :ping (reset! last-ping (System/currentTimeMillis))
      :on-key-down (shortcuts/keydown event)
      (if-let [f (get-in @handlers [(some-> id parse-uuid) op])]
        (f event)
        (when-not (= :on-visible op)
          (tap> [:missing-handler event]))))))

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

(def ^:private timeout-ms
  "Browsers can throttle `js/setInteval` to once per 60 seconds, so this timeout
  should take that into account."
  120000)
(def ^:private budget-ms (/ 1000.0 30))

(declare on-close)

(defn- sleep [start]
  (let [total-time (- (System/currentTimeMillis) start)]
    (when (< total-time budget-ms)
      (let [sleep-time (long (- budget-ms total-time))]
        (Thread/sleep sleep-time)))))

(defn- start-render-loop [{:keys [last-ping channel] :as session} render]
  (let [running (atom true)]
    (future
      (start-profile session)
      (loop [state nil]
        (when @running
          (recur
           (let [start (System/currentTimeMillis)]
             (if (< timeout-ms (- start @last-ping))
               (do
                 (reset! running false)
                 (on-close session)
                 (server/close channel))
               (try
                 (render state)
                 (catch Exception e
                   (tap> [state e])
                   (reset! running false))
                 (finally
                   (sleep start)))))))))
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
     (server/send! channel (cond-> message (not (string? message)) (json/write))))))

(defn- render-app [{:keys [handlers selection-index output-buffer log] :as session}
                   {:keys [hiccup styles app-state] :as render-state}]
  (binding [rt/*session* session
            shortcuts/*log* log
            select/*selection-index* selection-index]
    (process-event-queue! session)
    (let [app-state' (some-> hiccup meta :state deref)]
      (if (and (some? app-state) (= app-state app-state'))
        render-state
        (let [cache (atom styles)]
          (binding [d/*cache* cache]
            (let [hiccup'
                  (if-not hiccup
                    (react/render [app/app session])
                    (react/render (meta hiccup) [app/app session]))]
              (when (not (identical? styles @cache))
                (send! session {:op "on-styles" :append-styles (->style (apply dissoc @cache (keys styles)))}))
              (when-not (= hiccup hiccup')
                (vreset! handlers {})
                (let [written-bytes
                      (binding [hiccup/*handlers* handlers]
                        (hiccup/html! output-buffer hiccup'))]
                  (send! session (hiccup/->input-stream output-buffer written-bytes))))
              {:hiccup hiccup' :styles @cache :app-state app-state'})))))))

(defn- on-open [session]
  (add-watch (:state session) :selected #'state/send-selected-values)
  (swap! rt/connections assoc (:session-id session) (partial send! session))
  (swap! render-loops update (:session-id session)
         (fn [stop-render-loop]
           (when (fn? stop-render-loop)
             (stop-render-loop))
           (start-render-loop session (partial #'render-app session)))))

(defn- on-receive [session message]
  (swap! (:event-queue session) conj (json/read message)))

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

(defn close [session-id]
  (when-let [send! (get @rt/connections session-id)]
    (send! {:op "on-close"})))

(defn clear [session-id]
  (if-let [value (get-in @rt/sessions [session-id :options :value])]
    (when (instance? clojure.lang.Atom value)
      (swap! value empty))
    (swap! @#'rt/tap-list empty)))

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
             :last-ping (atom (System/currentTimeMillis))
             :log (atom nil)
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
   (slurp (io/resource "portal/ui/ssr.cljs"))})

(defn- ->host [request]
  (str (case (:scheme request)
         :http "http://" :https "https://")
       (:server-name request) ":" (:server-port request)))

(defn- resolve-vendor*
  "Fetch assets for vendoring or caching.
   Additionally, re-write css urls to be vendor-able as well."
  [request]
  (let [url (subs (:query-string request) 4)]
    (if-not (str/ends-with? url ".css")
      (io/input-stream (io/as-url url))
      (let [host (->host request) uri (URI. url)]
        (-> (io/as-url url)
            (io/input-stream)
            (slurp)
            (str/replace
             #"url\(([^)]*)\)"
             (fn [[_ path]]
               (str "url(" host "/vendor?url=" (.resolve uri ^String path) ")"))))))))

(defn- resolve-vendor
  "Resolve vendor'd asset via classpath or cache via .portal/vendor"
  [request]
  (let [url  (subs (:query-string request) 4)
        path (subs (.getPath (URI. url)) 1)]
    (or (io/resource (fs/join "portal" "vendor" path))
        (let [file (fs/join (fs/cwd) ".portal" "vendor" path)]
          (when-not (fs/exists file)
            (fs/mkdir (fs/dirname file))
            (io/copy (resolve-vendor* request) (io/file file)))
          (io/file file)))))

(defmethod route [:get "/vendor"] [request]
  {:status  200
   :headers {"Access-Control-Allow-Origin" "*"
             "Cache-Control" "max-age=31536000, immutable"}
   :body    (io/input-stream (resolve-vendor request))})

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
;; [x] figure out how to collapse ssr module back into existing code
;; [x] fix deref pause not working sometimes
;; [x] fix :launcher :vs-code dependency on portal.runtime.index
;; [x] profile / optimize rendeing perf
;; [x] portal.api/sessions
;; [x] portal.api/clear
;; [x] portal.api/close
;; [x] portal.api/selected
;; [ ] portal.api/eval-str
;; [x] fix remote vs code websocket never disconnecting issue