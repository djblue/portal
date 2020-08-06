(ns portal.server
  (:require [clojure.java.io :as io]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.transit :as t]))

(defn- send-rpc [channel value]
  (server/send!
   channel
   {:status 200
    :headers {"Content-Type"
              "application/transit+json; charset=utf-8"}
    :body (try
            (t/edn->json
             (assoc value :portal.rpc/exception nil))
            (catch Exception e
              (t/edn->json
               {:portal/state-id (:portal/state-id value)
                :portal.rpc/exception e})))}))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- rpc-handler [request]
  (server/with-channel request channel
    (let [body  (t/json-stream->edn (:body request))
          op    (get rt/ops (:op body) not-found)
          done  #(send-rpc channel %)
          f     (op body done)]
      (when (fn? f)
        (server/on-close channel f)))))

(defn- send-resource [content-type resource-name]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    (-> resource-name io/resource slurp)})

(defn handler [request]
  (let [paths
        {"/"        #(send-resource "text/html"       "index.html")
         "/main.js" #(send-resource "text/javascript" "main.js")
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (when (fn? f) (f))))

(defn start [handler]
  (server/run-server handler {:port 0 :join? false}))

(defn stop [server] (server :timeout 1000))
