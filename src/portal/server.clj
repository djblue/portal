(ns portal.server
  (:require [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [org.httpkit.server :as server]
            [io.aviso.exception :as ex]
            [portal.runtime :as rt])
  (:import [java.io ByteArrayOutputStream]
           [java.util UUID]))

(defn instance->uuid [instance]
  (let [k [:instance instance]]
    (-> rt/instance-cache
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [uuid (UUID/randomUUID)]
               (assoc cache [:uuid uuid] instance k uuid)))))
        (get k))))

(defn uuid->instance [uuid]
  (get @rt/instance-cache [:uuid uuid]))

(defn var->symbol [v]
  (let [m (meta v)]
    (with-meta (symbol (str (:ns m)) (str (:name m))) m)))

(defn value->transit-stream [value out]
  (let [writer
        (transit/writer
         out
         :json
         {:handlers
          {clojure.lang.Var
           (transit/write-handler "portal.transit/var" var->symbol)
           java.net.URL
           (transit/write-handler "r" str)
           java.lang.Throwable
           (transit/write-handler "portal.transit/exception" #(ex/analyze-exception % nil))}
          :transform transit/write-meta
          :default-handler
          (transit/write-handler
           "portal.transit/object"
           (fn [o]
             (with-meta
               {:id (instance->uuid o) :type (pr-str (type o)) :string (pr-str o)}
               (meta o))))})]
    (transit/write writer value)
    (.toString out)))

(defn transit-stream->value [in]
  (transit/read
   (transit/reader
    in
    :json
    {:handlers
     {"portal.transit/var" (transit/read-handler find-var)
      "portal.transit/object" (transit/read-handler (comp uuid->instance :id))}})))

(defn value->transit [value]
  (let [out (ByteArrayOutputStream. (* 10 1024 1024))]
    (value->transit-stream value out)
    (.toString out)))

(defn send-rpc [channel value]
  (server/send!
   channel
   {:status 200
    :headers {"Content-Type"
              "application/transit+json; charset=utf-8"}
    :body (try
            (value->transit
             (assoc value :portal.rpc/exception nil))
            (catch Exception e
              (value->transit
               {:portal/state-id (:portal/state-id value)
                :portal.rpc/exception e})))}))

(defn not-found [_request done]
  (done {:status :not-found}))

(defn rpc-handler [request]
  (server/with-channel request channel
    (let [body  (transit-stream->value (:body request))
          op    (get rt/ops (:op body) not-found)
          done  #(send-rpc channel %)
          f     (op body done)]
      (when (fn? f)
        (server/on-close channel f)))))

(defn send-resource [content-type resource-name]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    (-> resource-name io/resource io/file)})

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
