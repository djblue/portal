(ns ^:no-doc portal.runtime.jvm.launcher
  (:require [clojure.edn :as edn]
            [org.httpkit.client :as client]
            [org.httpkit.server :as http]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server]))

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

(defmethod browser/-open :intellij [args] (remote-open (assoc args :config-file "intellij.edn")))
(defmethod browser/-open :vs-code  [args] (remote-open (assoc args :config-file "vs-code.edn")))
(defmethod browser/-open :electron [args] (remote-open (assoc args :config-file "electron.edn")))

(defonce ^:private server (atom nil))

(defn start [options]
  (let [options (merge @rt/default-options options)]
    (or @server
        (let [{:keys [port host]
               :or {port 0 host "localhost"}} options
              http-server (http/run-server #'server/handler
                                           {:port port
                                            :max-ws (* 1024 1024 1024)
                                            :legacy-return-value? false})]
          (reset!
           server
           {:http-server http-server
            :port (http/server-port http-server)
            :host host})))))

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
  (swap! rt/sessions select-keys (keys @c/connections)))

(defn close [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/close})
    (c/request (:session-id portal) {:op :portal.rpc/close}))
  (when (or (= portal :all) (empty? @c/connections))
    (future
      (some-> server deref :http-server http/server-stop!))
    (reset! server nil))
  (swap! rt/sessions dissoc (:session-id portal))
  (swap! rt/sessions select-keys (keys @c/connections)))

(defn eval-str [portal code]
  (let [response (if (= portal :all)
                   (c/request {:op :portal.rpc/eval-str :code code})
                   (c/request (:session-id portal)
                              {:op :portal.rpc/eval-str :code code}))]
    (if-not (:error response)
      (:result response)
      (throw (ex-info (:message response)
                      {:code code :cause (:result response)})))))

(defn sessions []
  (for [session-id (keys @c/connections)] (c/make-atom session-id)))

(reset! rt/request c/request)
