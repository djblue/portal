(ns portal.client.common
  (:require
   [cognitect.transit :as transit]
   [portal.runtime.json :as json]))

(defn- transit-write
  [value]
  (transit/write
   (transit/writer :json {:transform transit/write-meta})
   value))

(defn ->submit [fetch]
  (fn submit
    ([value] (submit nil value))
    ([{:keys [encoding port host]
       :or   {encoding :edn
              host     "localhost"
              port     53755}}
      value]
     (fetch
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
          :edn     (binding [*print-meta* true]
                     (pr-str value)))})))))
