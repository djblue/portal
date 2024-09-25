(ns ^:no-doc portal.runtime.jvm.launcher
  (:require [clojure.edn :as edn]
            [org.httpkit.client :as client]
            [org.httpkit.server :as http]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server]
            [portal.runtime.shell :refer [spawn]]))

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
                   edn/read-string
                   (merge (when-let [config (:launcher-config options)]
                            config))))
         search-paths)
        (throw
         (ex-info
          (str "No config file found: " config-file)
          {:options      options
           :config-file  config-file
           :search-paths search-paths})))))

(defn- remote-open [{:keys [portal options server] :as args}]
  (let [config (get-config args)
        {:keys [status error] :as response}
        @(client/request
          {:url    (str "http://" (:host config) ":" (:port config) "/open")
           :method :post
           :body   (pr-str {:portal  (into {} portal)
                            :options (select-keys options [:window-title])
                            :server  (select-keys server [:host :port])})})]
    (when (or error (not= status 200))
      (throw (ex-info "Unable to open extension"
                      {:options  options
                       :config   config
                       :response (select-keys response [:body :headers :status])}
                      error)))))

(defmethod browser/-open :intellij [args]
  (try
    (remote-open (assoc args :config-file "intellij.edn"))
    (catch Exception e
      (throw
       (ex-info
        (str
         (ex-message e)
         ": Please ensure extension is installed and Portal tab is open.")
        (ex-data e))))))

(defmethod browser/-open :vs-code  [args] (remote-open (assoc args :config-file "vs-code.edn")))

(defmethod browser/-open :emacs [{:keys [portal server]}]
  (let [url (str "http://" (:host server) ":" (:port server) "?" (:session-id portal))]
    (spawn "emacsclient" "--no-wait" "--eval"
           (str "(xwidget-webkit-browse-url " (pr-str url) ")"))))

(defmethod browser/-open :electron [args] (remote-open (assoc args :config-file "electron.edn")))

(defmethod browser/-open :auto [args]
  (browser/-open
   (assoc-in args [:options :launcher]
             (cond
               (fs/exists ".portal/vs-code.edn") :vs-code
               (fs/exists ".portal/intellij.edn") :intellij))))

(defonce ^:private server (atom nil))

(defn start [options]
  (let [options (merge @rt/default-options options)]
    (or @server
        (let [{:keys [port host]
               :or {port 0 host "localhost"}} options
              http-server (http/run-server #'server/handler
                                           {:port port
                                            :max-body (* 1024 1024 1024)
                                            :max-ws (* 1024 1024 1024)
                                            :legacy-return-value? false})]
          (reset!
           server
           {:http-server http-server
            :port (http/server-port http-server)
            :host host})))))

(defn stop []
  (some-> server deref :http-server http/server-stop!)
  (reset! server nil))

(defn open
  ([options]
   (open nil options))
  ([portal options]
   (let [server (start options)]
     (browser/open {:portal portal :options options :server server}))))

(defn clear [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/clear})
    (c/request (:session-id portal) {:op :portal.rpc/clear}))
  (rt/cleanup-sessions))

(defn close [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/close})
    (c/request (:session-id portal) {:op :portal.rpc/close}))
  (rt/close-session (:session-id portal))
  (rt/cleanup-sessions))

(defn eval-str [portal msg]
  (let [response (if (= portal :all)
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
