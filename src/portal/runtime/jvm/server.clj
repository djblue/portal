(ns portal.runtime.jvm.server
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.remote.socket :as socket])
  (:import [java.util UUID]))

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
  (let [session (-> request
                    :session
                    (assoc :value-cache (atom {})))
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
        (swap! c/connections assoc (:session-id session) (partial send! ch)))
      :on-close
      (fn [_ch _status]
        (swap! c/connections dissoc (:session-id session)))})))

(defn- rpc-handler [request]
  (if (get-in request [:session :runtime])
    (rpc-handler-remote request)
    (rpc-handler-local request)))

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    resource})

(defn- wait []
  (try (Thread/sleep 60000)
       (catch Exception _e {:status 200})))

(defn- source-maps [request]
  (let [uri  (subs (:uri request) 1)
        file (io/file "target/resources/portal/" uri)]
    (when (and (str/includes? uri ".map") (.exists file))
      (send-resource "application/json" (slurp file)))))

(defn- main-js [request]
  {:status  200
   :headers {"Content-Type" "text/javascript"}
   :body
   (io/file
    (case (-> request :session :mode)
      :dev "target/resources/portal/main.js"
      (io/resource "portal/main.js")))})

(defn- get-session [request]
  (some->
   (or (:query-string request)
       (when-let [referer (get-in request [:headers "referer"])]
         (last (str/split referer #"\?"))))
   UUID/fromString))

(defn- with-session [request]
  (if-let [session-id (get-session request)]
    (assoc request :session
           (-> @rt/sessions
               (get session-id)
               (assoc :session-id session-id)))
    request))

(defn handler [request]
  (let [request (with-session request)
        paths
        {"/"        #(send-resource "text/html" (index/html))
         "/wait.js" wait
         "/main.js" #(main-js request)
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (cond
      (fn? f) (f)
      :else   (source-maps request))))
