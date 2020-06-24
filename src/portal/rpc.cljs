(ns portal.rpc
  (:require [cognitect.transit :as t]))

(defn json->edn [json]
  (let [r (t/reader :json)] (t/read r json)))

(defn edn->json [edn]
  (let [w (t/writer :json {:transform t/write-meta})]
    (t/write w edn)))

(defn send! [msg]
  (-> (js/fetch
       "/rpc"
       #js {:method "POST" :body (edn->json msg)})
      (.then #(.text %))
      (.then json->edn)))

