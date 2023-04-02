(ns ^:no-doc portal.client.common
  (:require [portal.runtime]
            [portal.runtime.cson :as cson]
            [portal.runtime.json :as json]
            [portal.runtime.transit :as transit]))

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
      {:method "POST"
       :headers
       {"content-type"
        (case encoding
          :json    "application/json"
          :cson    "application/cson"
          :transit "application/transit+json"
          :edn     "application/edn")}
       :body
       (case encoding
         :json    (json/write value)
         :transit (transit/write value)
         :cson    (cson/write value)
         :edn     (binding [*print-meta* true]
                    (pr-str value)))}))))
