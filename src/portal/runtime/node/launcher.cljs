(ns ^:no-doc portal.runtime.node.launcher
  (:require ["http" :as http]
            [clojure.edn :as edn]
            [portal.async :as a]
            [portal.client.node :as client]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.node.client :as c]
            [portal.runtime.node.server :as server]))

(defn- get-workspace-folder []
  (try
    (let [vscode (js/require "vscode")
          ^js uri (-> vscode .-workspace .-workspaceFolders (aget 0) .-uri)
          fs-path (.-fsPath uri)]
      (if-not (undefined? fs-path) fs-path (.-path uri)))
    (catch :default _e)))

(defn- get-search-paths [base-path]
  (->> base-path (iterate fs/dirname) (take-while some?)))

(defn get-config [{:keys [options config-file]}]
  (let [search-paths (concat (get-search-paths (fs/cwd))
                             (some-> (get-workspace-folder) get-search-paths))]
    (or (some
         (fn [parent]
           (some-> parent
                   (fs/join ".portal" config-file)
                   fs/exists
                   fs/slurp
                   edn/read-string))
         search-paths)
        (throw
         (ex-info
          (str "No config file found: " config-file)
          {:options      options
           :config-file  config-file
           :search-paths search-paths})))))

(defn- localhost
  "https://github.com/nodejs/node/issues/40537"
  [host]
  (if (= "localhost" host) "127.0.0.1" host))

(defn- remote-open [{:keys [portal options server] :as args}]
  (a/let [config (get-config args)
          {:keys [status error] :as response}
          (client/fetch
           (str "http://" (localhost (:host config)) ":" (:port config) "/open")
           {:method  "POST"
            :headers {"content-type" "application/edn"}
            :body    (pr-str {:portal  (into {} portal)
                              :options (select-keys options [:window-title])
                              :server  (select-keys server [:host :port])})})]
    (when (or error (not= status 200))
      (throw (ex-info "Unable to open extension"
                      {:options  options
                       :config   config
                       :response (select-keys response [:body :headers :status])}
                      error)))))

(defmethod browser/-open :intellij [args] (remote-open (assoc args :config-file "intellij.edn")))
(defmethod browser/-open :vs-code  [args] (remote-open (assoc args :config-file "vs-code.edn")))
(defmethod browser/-open :electron [args] (remote-open (assoc args :config-file "electron.edn")))

(defonce ^:private server (atom nil))
(defonce ^:private sockets (atom #{}))

(defn- create-server [handler port host]
  (js/Promise.
   (fn [resolve _reject]
     (let [^js server (http/createServer #(handler %1 %2))]
       (set! (.-requestTimeout server) 0)
       (set! (.-headersTimeout server) 0)
       (.on server
            "connection"
            (fn [^js socket]
              (swap! sockets conj socket)
              (.on socket
                   "close"
                   (fn []
                     (.destroy socket)
                     (swap! sockets disj socket)))))
       (.listen server #js {:port port :host (localhost host)}
                #(resolve {:http-server server
                           :port (.-port (.address server))
                           :host host}))))))

(defn start [options]
  (let [options (merge @rt/default-options options)]
    (or @server
        (a/let [{:keys [port host]
                 :or {port 0 host "localhost"}} options
                instance (create-server #'server/handler port host)]
          (reset! server instance)))))

(defn stop []
  (doseq [^js socket @sockets] (.destroy socket))
  (reset! sockets #{})
  (when-let [^js server (some-> server deref :http-server)]
    (.close server))
  (reset! server nil))

(defn open
  ([options]
   (open {:session-id (random-uuid)} options))
  ([portal options]
   (let [portal (or portal (c/make-atom (random-uuid)))]
     (a/let [server (start options)]
       (browser/open {:portal portal :options options :server server}))
     portal)))

(defn clear [portal]
  (a/do
    (if (= portal :all)
      (c/request {:op :portal.rpc/clear})
      (c/request (:session-id portal) {:op :portal.rpc/clear}))
    (rt/cleanup-sessions)))

(defn close [portal]
  (a/do
    (if (= portal :all)
      (c/request {:op :portal.rpc/close})
      (c/request (:session-id portal) {:op :portal.rpc/close}))
    (rt/close-session (:session-id portal))
    (rt/cleanup-sessions))
  true)

(defn eval-str [portal msg]
  (a/let [response (if (= portal :all)
                     (c/request (assoc msg :op :portal.rpc/eval-str))
                     (c/request (:session-id portal)
                                (assoc msg :op :portal.rpc/eval-str)))]
    (if-not (:error response)
      response
      (throw (ex-info (:message response) response)))))

(defn sessions []
  (for [session-id (rt/active-sessions)] (c/make-atom session-id)))

(defn url [portal]
  (browser/url {:portal portal :server @server}))

(reset! rt/request c/request)
