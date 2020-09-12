(ns portal.runtime.server.node
  (:require [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.client :as c]
            [portal.runtime.transit :as t]))

(defn- buffer-body [request]
  (js/Promise.
   (fn [resolve reject]
     (let [body (atom "")]
       (.on request "data" #(swap! body str %))
       (.on request "end"  #(resolve @body))
       (.on request "error" reject)))))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- send-rpc [response value]
  (-> response
      (.writeHead 200 #js {"Content-Type"
                           "application/transit+json; charset=utf-8"})
      (.end (try
              (t/edn->json
               (assoc value :portal.rpc/exception nil))
              (catch js/Error e
                (t/edn->json
                 {:portal/state-id (:portal/state-id value)
                  :portal.rpc/exception e}))))))

(def ops (merge c/ops rt/ops))

(defn- rpc-handler [request response]
  (a/let [body    (buffer-body request)
          req     (t/json->edn body)
          op      (get ops (get req :op) not-found)
          done    #(send-rpc response %)
          cleanup (op req done)]
    (when (fn? cleanup)
      (.on request "close" cleanup))))

(defn- send-resource [response content-type body]
  (-> response
      (.writeHead 200 #js {"Content-Type" content-type})
      (.end body)))

(defn handler [request response]
  (let [paths
        {"/"        #(send-resource response "text/html"       (io/resource "index.html"))
         "/main.js" #(send-resource response "text/javascript" (io/resource "main.js"))
         "/rpc"     #(rpc-handler request response)}

        f (get paths (.-url request) #(-> response (.writeHead 404) .end))]
    (when (fn? f) (f))))
