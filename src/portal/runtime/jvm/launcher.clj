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

(defn- sessions!
  ([f]
   (sessions! (rt/active-sessions) f))
  ([sessions f]
   (when-let [sessions (seq sessions)]
     (let [response (promise)]
       (doseq [session-id sessions]
         (future
           (try
             (deliver response (f session-id))
             (catch Exception ex
               (when (-> ex ex-data ::timeout)
                 (swap! rt/connections dissoc session-id))
               (deliver response ex)))))
       (let [response (deref response c/timeout ::timeout)]
         (cond
           (instance? Throwable response)
           (throw response)
           (not= response ::timeout)
           response
           :else
           (throw (ex-info
                   "Portal request timeout"
                   {::timeout true
                    :session-id :all}))))))))

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
  (if (= portal :all)
    (sessions! clear-1)
    (clear-1 (:session-id portal)))
  (rt/cleanup-sessions))

(defn- close-1 [session-id]
  (case (rt/session-mode session-id)
    :ssr ((requiring-resolve 'portal.ssr.server/close) session-id)
    (c/request session-id {:op :portal.rpc/close}))
  (rt/close-session session-id))

(defn close [portal]
  (if (= portal :all)
    (sessions! close-1)
    (close-1 (:session-id portal)))
  (rt/cleanup-sessions))

(defn- rpc-sessions []
  (filter #(= :rpc (rt/session-mode %)) (rt/active-sessions)))

(defn eval-str [portal msg]
  (let [response
        (if (= portal :all)
          (sessions!
           (rpc-sessions)
           #(c/request % (assoc msg :op :portal.rpc/eval-str)))
          (c/request (:session-id portal) (assoc msg :op :portal.rpc/eval-str)))]
    (if-not (:error response)
      response
      (throw (ex-info (:message response) response)))))

(defn sessions []
  (for [session-id (rt/active-sessions)] (c/make-atom session-id)))

(defn url [portal]
  (browser/url {:portal portal :server @server}))

(reset! rt/request c/request)
