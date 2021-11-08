(ns portal.client.jvm
  (:require
   [cognitect.transit :as transit]
   [org.httpkit.client :as http]
   [portal.runtime.json :as json])
  (:import [java.io ByteArrayOutputStream]))

(defn- transit-write [value]
  (let [out (ByteArrayOutputStream. 1024)]
    (transit/write
     (transit/writer out :json {:transform transit/write-meta})
     value)
    (.toString out)))

(defn send!
  ([value] (send! nil value))
  ([{:keys [encoding port host]
     :or   {encoding :transit
            host     "localhost"
            port     53755}}
    value]
   @(http/post
     (str "http://" host ":" port "/submit")
     {:headers
      {"content-type"
       (case encoding
         :json    "application/json"
         :transit "application/transit+json"
         :edn     "application/edn")}
      :body
      (case encoding
        :json    (json/write value)
        :transit (transit-write value)
        :edn     (pr-str value))})))

(comment
  (send! nil {:runtime 'jvm :value "hello jvm"})
  (send! {:port 1664} {:runtime 'jvm :value "hello jvm"})

  (add-tap send!)
  (tap> {:runtime 'jvm :value "hello jvm"})

  (add-tap (partial send! {:encoding :json}))
  (add-tap (partial send! {:encoding :edn})))
