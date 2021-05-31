(ns portal.runtime.jvm.server
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.jvm.client :as c])
  (:import [java.util UUID]))

(defn- not-found [_request done]
  (done {:status :not-found}))

(def ^:private ops (merge c/ops rt/ops))

(defn- rpc-handler [request]
  (let [session-id (UUID/fromString (:query-string request))]
    (server/as-channel
     request
     {:on-receive
      (fn [ch message]
        (let [body  (rt/read message)
              id    (:portal.rpc/id body)
              op    (get ops (:op body) not-found)]
          (future
            (op body #(server/send!
                       ch
                       (rt/write
                        (assoc %
                               :portal.rpc/id id
                               :op :portal.rpc/response)))))))
      :on-open
      (fn [ch]
        (swap! c/sessions
               assoc session-id
               (fn send! [message]
                 (server/send! ch (rt/write message)))))
      :on-close
      (fn [_ch _status] (swap! c/sessions dissoc session-id))})))

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
