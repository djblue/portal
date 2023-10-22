(ns ^:no-doc portal.runtime.jvm.server
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.cson :as cson]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.json :as json]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.npm :as npm]
            [portal.runtime.remote.socket :as socket])
  (:import [java.io File PushbackReader]
           [java.util UUID]))

(def ^:private enable-cors
  {:status 204
   :headers
   {"Access-Control-Allow-Origin"  "*"
    "Access-Control-Allow-Headers" "origin, content-type"
    "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
    "Access-Control-Max-Age"       86400}})

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

(defn- open-debug [{:keys [options] :as session}]
  (try
    (when (= :server (:debug options))
      ((requiring-resolve 'portal.runtime.debug/open) session))
    (catch Exception e (tap> e) nil)))

(defn- close-debug [instance]
  (try
    (when instance
      ((requiring-resolve 'portal.runtime.debug/close) instance))
    (catch Exception e (tap> e) nil)))

(defn- rpc-handler-local [request]
  (let [session (rt/open-session (:session request))
        send!   (fn send! [ch message]
                  (server/send! ch (rt/write message session)))
        debug   (open-debug session)]
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
        (swap! rt/connections assoc (:session-id session) (partial send! ch))
        (when-let [f (get-in session [:options :on-load])]
          (f)))
      :on-close
      (fn [_ch _status]
        (close-debug debug)
        (swap! rt/connections dissoc (:session-id session)))})))

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

(defmethod route :default [request]
  (if-not (str/ends-with? (:uri request) ".map")
    {:status 404}
    (let [uri (subs (:uri request) 1)]
      (some
       (fn [^File file]
         (when (and file (.exists file))
           (send-resource "application/json" (slurp file))))
       [(io/file (io/resource (str "portal-dev/" uri)))
        (io/file (io/resource uri))]))))

(defmethod route [:get "/icon.svg"] [_]
  {:status  200
   :headers {"Content-Type" "image/svg+xml"}
   :body (slurp (io/resource "portal/icon.svg"))})

(defmethod route [:get "/main.js"] [request]
  {:status  200
   :headers {"Content-Type" "text/javascript"}
   :body
   (slurp
    (io/resource
     (case (-> request :session :options :mode)
       :dev "portal-dev/main.js"
       "portal/main.js")))})

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

(defn- content-type [request]
  (some-> (get-in request [:headers "content-type"])
          (str/split #";")
          first))

(defn- body [{:keys [body] :as request}]
  (case (content-type request)
    "application/transit+json" (transit/read (transit/reader body :json))
    "application/json"         (json/read-stream (io/reader body))
    "application/cson"         (cson/read (slurp body))
    "application/edn"          (edn/read
                                {:default tagged-literal}
                                (PushbackReader. (io/reader body)))))

(defn- ->js [file]
  (let [source (fs/slurp file)]
    {:lang :js :npm true :file file :dir (fs/dirname file) :source source}))

(defn- node-resolve [{:keys [name parent]}]
  (if-not parent
    (some-> name npm/node-resolve ->js)
    (some-> name (npm/node-resolve parent) ->js)))

(defmethod route [:options "/load"] [_] enable-cors)
(defmethod route [:post "/load"] [request]
  {:headers
   {"content-type" "application/json"
    "Access-Control-Allow-Origin" "*"}
   :body
   (json/write
    (let [{:keys [name path macros] :as m} (body request)]
      (if (or (= name 'react) (string? name) (:npm name))
        (node-resolve m)
        (some
         (fn [ext]
           (when-let [resource (io/resource (str path ext))]
             {:lang (if (= ext ".js") :js :clj)
              :file (str resource)
              :source (slurp resource)}))
         (if macros
           [".clj"  ".cljc"]
           [".cljs" ".cljc" ".js"])))))})

(defmethod route [:options "/submit"] [_] enable-cors)
(defmethod route [:post "/submit"] [request]
  (rt/update-value (body request))
  {:status  204
   :headers {"Access-Control-Allow-Origin" "*"}})

(defmethod route [:get "/"] [request]
  (if-let [session (:session request)]
    (send-resource "text/html" (index/html (:options session)))
    (let [session-id (UUID/randomUUID)]
      (swap! rt/sessions assoc session-id {})
      {:status 307 :headers {"Location" (str "?" session-id)}})))

(defn handler [request] (route (with-session request)))
