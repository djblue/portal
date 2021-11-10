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

(defn submit
  ([value] (submit nil value))
  ([{:keys [encoding port host]
     :or   {encoding :edn
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
        :edn     (binding [*print-meta* true]
                   (pr-str value)))})))

(comment
  (submit {:runtime :jvm :value "hello jvm"})
  (submit {:port 1664} {:runtime :jvm :value "hello jvm"})

  (add-tap submit)
  (tap> {:runtime :jvm :value "hello jvm"})

  (add-tap (partial submit {:encoding :json}))
  (add-tap (partial submit {:encoding :transit})))
