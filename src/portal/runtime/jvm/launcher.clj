(ns ^:no-doc portal.runtime.jvm.launcher
  (:require [org.httpkit.server :as http]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server]))

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
   (when (= :ssr (:mode options)) (require 'portal.ssr.server))
   (let [server (start options)]
     (browser/open {:portal portal :options options :server server}))))

(defn- clear-1 [session-id]
  (case (rt/session-mode session-id)
    :ssr ((requiring-resolve 'portal.ssr.server/clear) session-id)
    (c/request session-id {:op :portal.rpc/clear})))

(defn clear [portal]
  (if-not (= portal :all)
    (clear-1 (:session-id portal))
    (doseq [session-id (rt/active-sessions)]
      (clear-1 session-id)))
  (rt/cleanup-sessions))

(defn- close-1 [session-id]
  (case (rt/session-mode session-id)
    :ssr ((requiring-resolve 'portal.ssr.server/close) session-id)
    (c/request session-id {:op :portal.rpc/close}))
  (rt/close-session session-id))

(defn close [portal]
  (if-not (= portal :all)
    (close-1 (:session-id portal))
    (doseq [session-id (rt/active-sessions)]
      (close-1 session-id)))
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
