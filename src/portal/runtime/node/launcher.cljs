(ns ^:no-doc portal.runtime.node.launcher
  (:require ["http" :as http]
            [portal.async :as a]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.node.client :as c]
            [portal.runtime.node.server :as server]))

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
  (or @server
      (a/let [{:portal.launcher/keys [port host]
               :or {port 0 host "localhost"}} options
              instance (create-server #'server/handler port host)]
        (reset! server instance))))

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

(defn clear []
  (c/request {:op :portal.rpc/clear})
  (swap! rt/sessions select-keys (keys @c/connections)))

(defn close []
  (a/do
    (c/request {:op :portal.rpc/close})
    (doseq [socket @sockets] (.destroy socket))
    (reset! sockets #{})
    (stop @server)
    (reset! server nil)
    (reset! rt/sessions {}))
  true)

(defn eval-str [code]
  (a/let [responses (c/request
                     {:op   :portal.rpc/eval-str
                      :code code})]
    (-> responses last :result)))

(reset! rt/request c/request)
