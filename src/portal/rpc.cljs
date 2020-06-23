(ns portal.rpc
  (:require [cognitect.transit :as t]
            [examples.hacker-news :as hn]
            [portal.async :as a]
            [clojure.datafy :refer [datafy nav]]))

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

(comment
  ;; example for browser only implementation
  (defn send! [msg]
    (js/Promise.resolve
     (case (:op msg)
       :portal.rpc/clear-values nil
       :portal.rpc/load-state
       {:portal/complete? true
        :portal/value
        {:stories hn/stories
         :uuid (random-uuid)
         :date (js/Date.)}}
       :portal.rpc/on-nav
       (a/let [res (apply nav (:args msg))]
         {:value (datafy res)})))))

