(ns portal.runtime.server.jvm
  (:require [clojure.java.io :as io]
            [clojure.datafy :refer [datafy]]
            [portal.runtime :as rt]
            [portal.runtime.client :as c]
            [portal.runtime.transit :as t]))

(defn- send-rpc [value]
  {:status 200
   :headers {"Content-Type"
             "application/transit+json; charset=utf-8"}
   :body (try
           (t/edn->json value)
           (catch Exception e
             (t/edn->json
              {:portal/state-id (:portal/state-id value)
               :portal/value (datafy (Exception. "Transit failed to encode a value. Clear portal to proceed." e))})))})

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- race [& promises]
  (let [winner  (promise)
        futures (for [p promises]
                  (future (deliver winner @p)))]
    (dorun futures)
    (let [winner @winner]
      (dorun (map future-cancel futures))
      winner)))

(def ^:private ops (merge c/ops rt/ops))

(defn- rpc-handler [request]
  (let [body  (t/json->edn (:body request))
        op    (get ops (:op body) not-found)
        p     (promise)
        done  #(deliver p (send-rpc %))
        f     (op body done)
        res   (race p (:closed? request))]
    (cond
      (fn? f)     (f true)
      (future? f) (future-cancel f))
    res))

(def ^:private resource
  {"main.js"    (io/resource "main.js")
   "index.html" (io/resource "index.html")})

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    (slurp resource)})

(defn- wait [] (Thread/sleep 60000))

(defn handler [request]
  (let [paths
        {"/"        #(send-resource "text/html"       (resource "index.html"))
         "/wait.js" wait
         "/main.js" #(send-resource "text/javascript" (resource "main.js"))
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (when (fn? f) (f))))

