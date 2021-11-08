(ns portal.client.common
  (:require
   [portal.runtime.json :as json]
   [cognitect.transit :as transit]))

(defn- transit-write
  [value]
  (transit/write
   (transit/writer :json {:transform transit/write-meta})
   value))

(defn ->send! [fetch-fn]
  (fn send!
    ([value] (send! nil value))
    ([{:keys [encoding port host]
       :or   {encoding :transit
              host     "localhost"
              port     53755}}
      value]
     (-> (fetch-fn
          (str "http://" host ":" port "/submit")
          (clj->js
           {:method "POST"
            :headers
            {"content-type"
             (case encoding
               :json    "application/json"
               :transit "application/transit+json"
               :edn     "application/edn")}
            :body
            (case encoding
              :json    (json/write value)
              :transit (transit-write value)
              :edn     (pr-str value))}))))))
