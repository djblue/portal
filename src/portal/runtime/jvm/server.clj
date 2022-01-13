(ns ^:no-doc portal.runtime.jvm.server
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.json :as json]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.remote.socket :as socket])
  (:import [java.io File PushbackReader]
           [java.util UUID]))

(defmulti route (juxt :request-method :uri))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- rpc-handler-remote [request]
  (let [conn (socket/open (:session request))]
    (server/as-channel
     request
     {:on-receive
      (fn [_ch message]
        (socket/handler conn message))
      :on-open
      (fn [ch]
        (try
          (doseq [message (socket/responses conn)]
            (server/send! ch message))
          (catch Exception _e)))
      :on-close
      (fn [_ch _status]
        (socket/close conn))})))

(def ^:private ops (merge c/ops rt/ops))

(defn- rpc-handler-local [request]
  (let [session (rt/open-session (:session request))
        send!   (fn send! [ch message]
                  (server/send! ch (rt/write message session)))]
    (server/as-channel
     request
     {:on-receive
      (fn [ch message]
        (let [body  (rt/read message session)
              id    (:portal.rpc/id body)
              op    (get ops (:op body) not-found)]
          (binding [rt/*session* session]
            (op body (fn [response]
                       (send!
                        ch
                        (assoc response
                               :portal.rpc/id id
                               :op :portal.rpc/response)))))))
      :on-open
      (fn [ch]
        (swap! c/connections assoc (:session-id session) (partial send! ch))
        (when-let [f (get-in session [:options :on-load])]
          (f)))
      :on-close
      (fn [_ch _status]
        (swap! c/connections dissoc (:session-id session)))})))

(defmethod route [:get "/rpc"] [request]
  (if (get-in request [:session :options :runtime])
    (rpc-handler-remote request)
    (rpc-handler-local request)))

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    resource})

(defmethod route [:get "/wait.js"] [_]
  (try (Thread/sleep 60000)
       (catch Exception _e {:status 200})))

(defn- ext [^File file]
  (first (re-find #"\.([^.]+)$" (.getName file))))

(def ^:private ext->mime
  {".map" "application/json"
   ".svg" "image/svg+xml"})

(defmethod route :default [request]
  (let [uri (subs (:uri request) 1)]
    (some
     (fn [^File file]
       (when (and file (.exists file))
         (send-resource (-> file ext ext->mime) (slurp file))))
     [(io/file "target/resources/portal/" uri)
      (io/file (io/resource uri))])))

(defmethod route [:get "/main.js"] [request]
  {:status  200
   :headers {"Content-Type" "text/javascript"}
   :body
   (case (-> request :session :options :mode)
     :dev (io/file "target/resources/portal/main.js")
     (slurp (io/resource "portal/main.js")))})

(defn- get-session-id [request]
  ;; There might be a referrer which is not a UUID in standalone mode.
  (try
    (some->
     (or (:query-string request)
         (when-let [referer (get-in request [:headers "referer"])]
           (last (str/split referer #"\?"))))
     UUID/fromString)
    (catch Exception _ nil)))

(defn- with-session [request]
  (if-let [session-id (get-session-id request)]
    (assoc request :session (rt/get-session session-id))
    request))

(defmethod route [:post "/submit"] [request]
  (let [body (:body request)]
    (rt/update-value
     (case (get-in request [:headers "content-type"])
       "application/transit+json" (transit/read (transit/reader body :json))
       "application/json"         (json/read-stream (io/reader body))
       "application/edn"          (edn/read
                                   {:default tagged-literal}
                                   (PushbackReader. (io/reader body)))))
    {:status 200}))

(defmethod route [:options "/submit"] [_]
  {:status 204
   :headers
   {"Access-Control-Allow-Origin"  "*"
    "Access-Control-Allow-Headers" "origin, content-type"
    "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
    "Access-Control-Max-Age"       86400}})

(defmethod route [:get "/"] [_]
  (send-resource "text/html" (index/html)))

(defn handler [request] (route (with-session request)))
