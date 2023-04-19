(ns ^:no-doc ^:no-check portal.runtime.clr.server
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.runtime :as rt]
            [portal.runtime.clr.client :as c]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.json :as json])
  (:import (clojure.lang RT)
           (System Environment Guid)
           (System.IO Path)
           (System.Net HttpListenerContext)
           (System.Net.WebSockets WebSocket WebSocketMessageType WebSocketState)
           (System.Text Encoding)
           (System.Threading CancellationToken Thread)))

(defmulti route (juxt :request-method :uri))

(def ^:private ops (merge c/ops rt/ops))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defmacro array-segment [& args]
  `(new ~(RT/classForName "System.ArraySegment`1[System.Byte]") ~@args))

(defn- send-message [^WebSocket ws ^String message]
  (let [bytes (.GetBytes Encoding/UTF8 message)]
    (.SendAsync
     ws
     (array-segment bytes)
     WebSocketMessageType/Text
     true
     CancellationToken/None)))

(defn- recieve-messgae [^WebSocket ws]
  (let [max-size (* 50 1024 1024)
        buffer   (byte-array max-size)]
    (loop [receive-count 0]
      (let [task   (.ReceiveAsync
                    ws
                    (array-segment buffer receive-count (- max-size receive-count))
                    CancellationToken/None)
            _      (.Wait task)
            result (.Result task)]
        (if-not (.EndOfMessage result)
          (recur (+ receive-count (.Count result)))
          (.GetString Encoding/UTF8 buffer 0 (+ (.Count result) receive-count)))))))

(defmethod route [:get "/rpc"] [request]
  (future
    (let [session (rt/open-session (:session request))]
      (try
        (let [^HttpListenerContext context (:context request)
              task  (.AcceptWebSocketAsync context nil)
              _     (.Wait task)
              ws    (.WebSocket (.Result task))
              send! (fn [message]
                      (send-message ws (rt/write message session)))]
          (swap! c/connections assoc (:session-id session) send!)
          (when-let [f (get-in session [:options :on-load])]
            (f))
          (while (= (.State ws) WebSocketState/Open)
            (when-let [message (not-empty (recieve-messgae ws))]
              (let [body     (rt/read message session)
                    id       (:portal.rpc/id body)
                    op       (get ops (:op body) not-found)]
                (binding [rt/*session* session]
                  (op body (fn [response]
                             (send!
                              (assoc response
                                     :portal.rpc/id id
                                     :op :portal.rpc/response)))))))))
        (catch Exception e
          (tap> (Throwable->map e)))
        (finally
          (swap! c/connections dissoc (:session-id session)))))))

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    resource})

(defmethod route [:get "/wait.js"] [_]
  (try (Thread/Sleep 60000)
       (catch Exception _e {:status 200})))

(defn- resource [path]
  (some
   (fn [dir]
     (fs/exists (fs/join (fs/cwd) dir path)))
   (str/split
    (Environment/GetEnvironmentVariable "CLOJURE_LOAD_PATH")
    (re-pattern (str Path/PathSeparator)))))

(defmethod route :default [request]
  (if-not (str/ends-with? (:uri request) ".map")
    {:status 404}
    (let [uri (subs (:uri request) 1)]
      (some
       (fn [file]
         (when file
           (send-resource "application/json" (slurp file))))
       [(resource (str "portal-dev/" uri))
        (resource uri)]))))

(defmethod route [:get "/icon.svg"] [_]
  {:status  200
   :headers {"Content-Type" "image/svg+xml"}
   :body (fs/slurp (resource "portal/icon.svg"))})

(defmethod route [:get "/main.js"] [request]
  {:status  200
   :headers {"Content-Type" "text/javascript"}
   :body
   (fs/slurp
    (resource
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
     str
     Guid/Parse)
    (catch Exception _ nil)))

(defn- with-session [request]
  (if-let [session-id (get-session-id request)]
    (assoc request :session (rt/get-session session-id))
    request))

(defn- content-type [request]
  (some-> (get-in request [:headers "content-type"])
          (str/split #";")
          first))

(defmethod route [:post "/submit"] [request]
  (let [body (:body request)]
    (rt/update-value
     (case (content-type request)
       "application/json"         (json/read body)
       "application/edn"          (edn/read-string {:default tagged-literal} body)))
    {:status  204
     :headers {"Access-Control-Allow-Origin" "*"}}))

(defmethod route [:options "/submit"] [_]
  {:status 204
   :headers
   {"Access-Control-Allow-Origin"  "*"
    "Access-Control-Allow-Headers" "origin, content-type"
    "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
    "Access-Control-Max-Age"       86400}})

(defmethod route [:get "/"] [request]
  (if-let [session (:session request)]
    (send-resource "text/html" (index/html (:options session)))
    (let [session-id (Guid/NewGuid)]
      (swap! rt/sessions assoc session-id {})
      {:status 307 :headers {"Location" (str "?" session-id)}})))

(defn handler [request] (route (with-session request)))
