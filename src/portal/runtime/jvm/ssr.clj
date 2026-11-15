(ns ^:no-doc portal.runtime.jvm.ssr
  (:refer-clojure :exclude [parse-uuid random-uuid])
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [org.httpkit.server :as server]
   [portal.runtime :as rt]
   [portal.runtime.fs :as fs]
   [portal.runtime.index :as index]
   [portal.runtime.json :as json]
   [portal.runtime.jvm.hiccup :as hiccup]
   [portal.runtime.jvm.server :refer [enable-cors route]]
   [portal.runtime.polyfill :refer [parse-uuid random-uuid]]
   [portal.runtime.react :as react]
   [portal.shortcuts :as shortcuts]
   [portal.ui.app :as app]
   [portal.ui.select :as select]
   [portal.ui.state :as state]
   [portal.ui.styled :as d])
  (:import
   [java.io ByteArrayInputStream]
   [java.net URI]))

(defn- on-message [{:keys [handlers last-ping] :as session} {:keys [id] :as event}]
  (let [op (keyword (:op event))]
    (case op
      :ping (reset! last-ping (System/currentTimeMillis))
      :on-response
      (let [rpc (:rpc session) id (some-> id parse-uuid)]
        (if-let [p (get @rpc id)]
          (do (swap! rpc dissoc id) (deliver p event))
          (tap> [:invalid-rpc event])))
      :on-key-down
      (if-let [f (get-in @handlers [(some-> id parse-uuid) op])]
        (f event)
        (shortcuts/keydown event))
      (if-let [f (get-in @handlers [(some-> id parse-uuid) op])]
        (f event)
        (when-not (= :on-visible op)
          (tap> [:missing-handler event]))))))

(declare send!)

(defn- rpc* [session {:keys [method params]}]
  (let [p (promise)
        id (random-uuid)]
    (swap! (:rpc session) assoc id p)
    (send! (:session-id session)
           {:op "on-rpc"
            :id (str id)
            :method (str (namespace method) "/" (name method))
            :params params})
    p))

(defonce ^:private sessions (atom {}))

(defn- get-session [session-id] (get @sessions session-id))

(defn- get-connections [session-id] (get-in @sessions [session-id :connections]))

(defn- start-profile [session-id]
  (when (get-in (get-session session-id) [:options :profile?])
    (try
      (let [start (requiring-resolve 'clj-async-profiler.core/start)]
        (start {:event :alloc}))
      (catch Exception e (tap> (Throwable->map e))))))

(defn- stop-profile [session-id]
  (when (get-in (get-session session-id) [:options :profile?])
    (try
      (let [browse-url (requiring-resolve 'clojure.java.browse/browse-url)
            stop (requiring-resolve 'clj-async-profiler.core/stop)]
        (browse-url (stop)))
      (catch Exception e (tap> (Throwable->map e))))))

(def ^:private timeout-ms
  "Browsers can throttle `js/setInterval` to once per 60 seconds, so this timeout
  should take that into account."
  120000)
(def ^:private budget-ms (/ 1000.0 30))

(defn- sleep [start]
  (let [total-time (- (System/currentTimeMillis) start)]
    (when (< total-time budget-ms)
      (let [sleep-time (long (- budget-ms total-time))]
        (Thread/sleep sleep-time)))))

(defn- start-render-loop [session-id render stop]
  (let [running (atom true)]
    (future
      (start-profile session-id)
      (loop [state nil]
        (let [{:keys [last-ping] :as session} (get-session session-id)]
          (if-not @running
            (stop state)
            (recur
             (let [start (System/currentTimeMillis)]
               (if (< timeout-ms (- start @last-ping))
                 (do
                   (reset! running false)
                   (doseq [[_ channel] (:connections session)]
                     (server/close channel))
                   state)
                 (try
                   (render state)
                   (catch Exception e
                     (tap> [state e])
                     (reset! running false)
                     state)
                   (finally
                     (sleep start))))))))))
    (fn stop-render-loop []
      (reset! running false)
      (stop-profile session-id))))

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
   (send! (:session-id rt/*session*) message))
  ([session-id message]
   (doseq [[_ channel] (get-connections session-id)]
     (send! session-id channel message)))
  ([_ channel message]
   (if (fn? channel)
     (channel message)
     (if (or (bytes? message)
             (instance? ByteArrayInputStream message))
       (server/send! channel message)
       (server/send! channel (cond-> message (not (string? message)) (json/write)))))))

(defn- render-app [session-id {:keys [hiccup styles app-state connections] :as render-state}]
  (let [{:keys [handlers selection-index output-buffer log] :as session} (get-session session-id)]
    (binding [rt/*session* session
              shortcuts/*log* log
              select/*selection-index* selection-index]
      (process-event-queue! session)

      ;; The app has already started and new connections have shown up.
      (when-let [written-bytes (:written-bytes render-state)]
        (let [all-styles (delay {:op "on-styles" :append-styles (->style styles)})]
          (doseq [[connection-id channel] (:connections session)
                  :when (not (contains? connections connection-id))]
            (send! session-id channel @all-styles)
            (send! session-id channel (hiccup/->input-stream output-buffer written-bytes)))))

      (let [app-state' (some-> hiccup meta :state deref)]
        (-> (if (and (some? app-state) (= app-state app-state'))
              render-state
              (let [cache (atom styles)]
                (binding [d/*cache* cache]
                  (let [hiccup'
                        (if-not hiccup
                          (react/render [app/app session])
                          (react/render (meta hiccup) [app/app session]))]
                    (when-not (identical? styles @cache)
                      (let [new-styles {:op "on-styles" :append-styles (->style (apply dissoc @cache (keys styles)))}]
                        (doseq [[_ channel] (:connections session)]
                          (send! session-id channel new-styles))))
                    (if (= hiccup hiccup')
                      (assoc render-state :hiccup hiccup' :styles @cache :app-state app-state')
                      (do
                        (vreset! handlers {})
                        (let [written-bytes
                              (binding [hiccup/*handlers* handlers]
                                (hiccup/html! output-buffer hiccup'))]
                          (doseq [[_ channel] (:connections session)]
                            (send! session-id channel (hiccup/->input-stream output-buffer written-bytes)))
                          {:hiccup hiccup'
                           :styles @cache
                           :app-state app-state'
                           :written-bytes written-bytes})))))))
            (assoc :connections (:connections session)))))))

(defn- stop-app [{:keys [hiccup]}]
  (when hiccup (react/render (meta hiccup) nil)))

(defn- open-session [session-id opts]
  (-> sessions
      (swap! update session-id
             (fn [session]
               (or session
                   (merge
                    opts
                    {:rpc (atom {})
                     :state (atom {})
                     :log (atom nil)
                     :handlers (volatile! {})
                     :event-queue (atom [])
                     :selection-index (atom {})
                     :last-ping (atom (System/currentTimeMillis))
                     :output-buffer (byte-array (* 5 1024 1024))}))))
      (get session-id)))

(defn- close-session [session-id]
  (swap! sessions dissoc session-id))

(defn- on-open [session-id opts]
  (let [session (open-session session-id opts)]
    (add-watch (:state session) :selected #'state/send-selected-values)
    (swap! rt/connections assoc session-id (partial send! session-id))
    (swap! render-loops update session-id
           (fn [stop-render-loop]
             (when (fn? stop-render-loop)
               (stop-render-loop))
             (start-render-loop session-id (partial #'render-app session-id) #'stop-app)))))

(defn- on-receive [session-id message]
  (when-let [event-queue (:event-queue (get-session session-id))]
    (swap! event-queue conj (json/read message))))

(defn- on-close [session-id]
  (let [session (get-session session-id)]
    (remove-watch (:state session) :selected)
    (swap! rt/connections dissoc session-id)
    (when-let [stop-render-loop (get @render-loops session-id)]
      (stop-render-loop)
      (swap! render-loops dissoc session-id))
    (close-session session-id)))

(defn close
  ([]
   (close (:session-id rt/*session*)))
  ([session-id]
   (when-let [send! (get @rt/connections session-id)]
     (send! {:op "on-close"}))))

(defn clear [session-id]
  (binding [rt/*session* (get-in @rt/sessions [session-id])]
    (rt/clear-values)))

(defn- on-connect [session-id connection-id ch]
  (swap! sessions assoc-in [session-id :connections connection-id] ch))

(defn- on-disconnect [session-id connection-id]
  (swap! sessions update-in [session-id :connections] dissoc connection-id))

(defn- ->opts [{:keys [session] :as request}]
  (cond-> session
    (some-> request
            (get-in [:headers "origin"])
            (str/starts-with? "vscode-webview://"))
    (update :options
            (fn [options]
              (cond-> options
                (nil? (:theme options))
                (assoc :theme :portal.colors/vs-code-embedded))))))

(defmethod route [:get "/ssr"] [{{:keys [session-id]} :session :as request}]
  (let [connection-id (random-uuid)]
    (server/as-channel
     request
     {:on-open    (fn [ch]
                    (locking sessions
                      (when (zero? (count (get-connections session-id)))
                        (on-open session-id (->opts request)))
                      (on-connect session-id connection-id ch)))
      :on-receive (fn [_ch message]
                    (on-receive session-id message))
      :on-close   (fn [_ch _status]
                    (locking sessions
                      (on-disconnect session-id connection-id)
                      (when (zero? (count (get-connections session-id)))
                        (on-close session-id))))})))

(defmethod route [:options "/main.cljs"] [_] enable-cors)
(defmethod route [:get "/main.cljs"] [_]
  {:status  200
   :headers {"Access-Control-Allow-Origin" "*"}
   :body
   (str
    (slurp (io/resource "portal/ui/macros.cljc"))
    (slurp (io/resource "portal/ui/web_components.cljs"))
    (slurp (io/resource "portal/ui/ssr.cljs")))})

(defn- resolve-vendor*
  "Fetch assets for vendoring or caching.
   Additionally, re-write css urls to be vendor-able as well."
  [request]
  (let [url (subs (:query-string request) 4)]
    (if-not (str/ends-with? url ".css")
      (io/input-stream (io/as-url url))
      (let [uri (URI. url)]
        (-> (io/as-url url)
            (io/input-stream)
            (slurp)
            (str/replace
             #"url\(([^)]*)\)"
             (fn [[_ path]]
               (str "url(/vendor?url=" (.resolve uri ^String path) ")"))))))))

(defn- resolve-vendor
  "Resolve vendor'd asset via classpath or cache via .portal/vendor"
  [request]
  (let [url  (subs (:query-string request) 4)
        path (subs (.getPath (URI. url)) 1)]
    (or (io/resource (str "portal/vendor/" path))
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

(defn- rpc
  ([method]
   (rpc rt/*session* method nil))
  ([method params]
   (rpc rt/*session* method params))
  ([session method params]
   (let [message {:method method :params params}
         {:keys [result error]}
         (-> (rpc* session message)
             (deref 1000 {:error {:message "RPC Timeout"}}))]
     (if-not error
       result
       (let [{:keys [^String message data]} error]
         (throw (if-not data (Exception. message) (ex-info message data))))))))

(defn clipboard [] (rpc :portal.ui.ssr/clipboard))

(defn- extract-vendor-url [resource]
  (re-seq #"/vendor\?(url=[^\")#]+)" resource))

(defn- download-vendor-urls [resource]
  (doseq [[_ query-string] (extract-vendor-url resource)
          :let [{:keys [body]}
                (route {:request-method :get
                        :uri "/vendor"
                        :query-string query-string})]]
    (println " ->" query-string)
    (download-vendor-urls (slurp body))))

(defn vendor []
  (println "=> Vendoring SSR resources")
  (download-vendor-urls (index/html {:mode :ssr})))