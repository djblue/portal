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

(defn- get-search-paths []
  (->> (fs/cwd) (iterate fs/dirname) (take-while some?)))

(defn get-config [{:keys [options config-file]}]
  (let [search-paths (get-search-paths)]
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

(defn- remote-open [{:keys [portal options server] :as args}]
  (a/let [config (get-config args)
          {:keys [status error] :as response}
          (client/fetch
           (str "http://" (:host config) ":" (:port config) "/open")
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
     (let [server (http/createServer #(handler %1 %2))]
       (.on server
            "connection"
            (fn [socket]
              (swap! sockets conj socket)
              (.on socket
                   "close"
                   (fn [] (swap! sockets disj sockets)))))
       (.listen server #js {:port port :host host}
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

(defn- stop [handle]
  (some-> handle :http-server .close))

(defn open
  ([options]
   (open {:session-id (random-uuid)} options))
  ([portal options]
   (let [portal (or portal (c/make-atom (random-uuid)))]
     (a/let [server (start options)]
       (browser/open {:portal portal :options options :server server}))
     portal)))

(defn clear [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/clear})
    (c/request (:session-id portal) {:op :portal.rpc/clear}))
  (swap! rt/sessions select-keys (keys @c/connections)))

(defn close [portal]
  (a/do
    (if (= portal :all)
      (c/request {:op :portal.rpc/close})
      (c/request (:session-id portal) {:op :portal.rpc/close}))
    (when (or (= portal :all) (empty? @c/connections))
      (doseq [socket @sockets] (.destroy socket))
      (reset! sockets #{})
      (stop @server)
      (reset! server nil))
    (swap! rt/sessions dissoc (:session-id portal))
    (swap! rt/sessions select-keys (keys @c/connections)))
  true)

(defn eval-str [portal msg]
  (a/let [responses (if (= portal :all)
                      (c/request (assoc msg :op :portal.rpc/eval-str))
                      (c/request (:session-id portal)
                                 (assoc msg :op :portal.rpc/eval-str)))
          response (last responses)]
    (if-not (:error response)
      response
      (throw (ex-info (:message response) response)))))

(defn sessions []
  (for [session-id (key @c/connections)] (c/make-atom session-id)))

(defn url [portal]
  (browser/url {:portal portal :server @server}))

(reset! rt/request c/request)
