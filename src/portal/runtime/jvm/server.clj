(ns portal.runtime.jvm.server
  (:require [clojure.datafy :refer [datafy]]
            [clojure.java.io :as io]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.transit :as t])
  (:import [java.util UUID]))

(defn- edn->json [value]
  (try
    (t/edn->json value)
    (catch Exception e
      (t/edn->json
       {:portal.rpc/id (:portal.rpc/id value)
        :op :portal.rpc/response
        :portal/state-id (:portal/state-id value)
        :portal/value (datafy (Exception. "Transit failed to encode a value. Clear portal to proceed." e))}))))

(defn- not-found [_request done]
  (done {:status :not-found}))

(def ^:private ops (merge c/ops rt/ops))

(defn- rpc-handler [request]
  (let [session-id (UUID/fromString (:query-string request))]
    (server/as-channel
     request
     {:on-receive
      (fn [ch message]
        (let [body  (t/json->edn message)
              id    (:portal.rpc/id body)
              op    (get ops (:op body) not-found)]
          (future
            (op body #(server/send!
                       ch
                       (edn->json
                        (assoc %
                               :portal.rpc/id id
                               :op :portal.rpc/response)))))))
      :on-open
      (fn [ch]
        (swap! c/sessions
               assoc session-id
               (fn send! [message]
                 (server/send! ch (edn->json message)))))
      :on-close
      (fn [_ch _status] (swap! c/sessions dissoc session-id))})))

(def ^:private resource
  {"main.js"    (io/resource "main.js")
   "index.html" (io/resource "index.html")})

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    (slurp resource)})

(defn- wait []
  (try (Thread/sleep 60000)
       (catch Exception _e {:status 200})))

(defn handler [request]
  (let [paths
        {"/"        #(send-resource "text/html"       (resource "index.html"))
         "/wait.js" wait
         "/main.js" #(send-resource "text/javascript" (resource "main.js"))
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (when (fn? f) (f))))

