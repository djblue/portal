(ns portal.client.web
  (:require
   [cognitect.transit :as transit]))

(defn- transit-write
  [value]
  (transit/write
   (transit/writer :json {:transform transit/write-meta})
   value))

(defn send!
  ([value] (send! nil value))
  ([{:keys [encoding port host]
     :or   {encoding :transit
            host     "localhost"
            port     53755}}
    value]
   (-> (js/fetch
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
            :json    (js/JSON.stringify value)
            :transit (transit-write value)
            :edn     (pr-str value))})))))

(comment

  (send! nil "Hello World")
  (add-tap send!)
  (tap> #?(:cljs {:runtime 'cljs :value "hello web"}))
  (add-tap send!)

  (add-tap (partial send! {:encoding :json}))
  (add-tap (partial send! {:encoding :edn})))
