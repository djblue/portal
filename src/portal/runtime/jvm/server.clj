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

(def ^:private ops (merge c/ops rt/ops))

(def options {:hostname "localhost" :port 5555})

(defn rpc-handler-remote [request]
  (socket/setup options)
  (let [socket (socket/create-socket options)]
    (server/as-channel
     request
     {:on-receive
      (fn [_ch message]
        (socket/handler socket message))
      :on-open
      (fn [ch]
        (doseq [message (socket/responses socket)]
          (server/send! ch message)))
      :on-close
      (fn [_ch _status]
        (socket/quit socket))})))

(defn- rpc-handler-local [request]
  (let [session-id (UUID/fromString (:query-string request))
        options    {:value-cache (atom {})}
        send!      (fn send! [ch message]
                     (server/send! ch (rt/write message options)))]
    (server/as-channel
     request
     {:on-receive
      (fn [ch message]
        (let [body  (rt/read message options)
              id    (:portal.rpc/id body)
              op    (get ops (:op body) not-found)]
          (binding [rt/*options* options]
            (op body (fn [response]
                       (send!
                        ch
                        (assoc response
                               :portal.rpc/id id
                               :op :portal.rpc/response)))))))
      :on-open
      (fn [ch]
        (swap! c/sessions assoc session-id (partial send! ch)))
      :on-close
      (fn [_ch _status] (swap! c/sessions dissoc session-id))})))

(defn- rpc-handler [request]
  (rpc-handler-local request))

(def ^:private resource
  {"main.js" (io/resource "portal/main.js")})

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

(defn handler [request]
  (let [paths
        {"/"        #(send-resource "text/html"       (index/html))
         "/wait.js" wait
         "/main.js" #(send-resource "text/javascript" (slurp (resource "main.js")))
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (cond
      (fn? f) (f)
      :else   (source-maps request))))
