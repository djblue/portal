(ns portal.runtime.jvm.launcher
  (:require [org.httpkit.server :as http]
            [portal.runtime.browser :as browser]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server]))

(defonce ^:private server (atom nil))

(defn start [options]
  (or @server
      (let [{:portal.launcher/keys [port host]
             :or {port 0 host "localhost"}} options
            http-server (http/run-server #'server/handler
                                         {:port port
                                          :max-ws (* 1024 1024 1024)
                                          :legacy-return-value? false})]
        (reset!
         server
         {:http-server http-server
          :port (http/server-port http-server)
          :host host}))))

(defn open
  ([options]
   (open nil options))
  ([portal options]
   (let [server (start options)]
     (browser/open {:portal portal :options options :server server}))))

(defn clear []
  (c/request {:op :portal.rpc/clear}))

(defn close []
  (c/request {:op :portal.rpc/close})
  (future
    (some-> server deref :http-server http/server-stop!))
  (reset! server nil))
