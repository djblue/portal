(ns portal.client.jvm
  (:require
   #?(:bb [cheshire.core :as json]
      :clj [clojure.data.json :as json])
   [cognitect.transit :as transit]
   [org.httpkit.client :as http])
  (:import [java.io ByteArrayOutputStream]))

(defn- ->json-str [v]
  (#?(:bb json/generate-string
      :clj json/write-str) v))

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
        :json    (->json-str value)
        :transit (transit-write value)
        :edn     (pr-str value))})))

(comment
  (send! nil #?(:bb {:runtime 'bb :value "hello bb"}
                :clj {:runtime 'jvm :value "hello jvm"}))

  (add-tap send!)
  (tap> #?(:bb {:runtime 'bb :value "hello bb"}
           :clj {:runtime 'jvm :value "hello jvm"}))

  (add-tap (partial send! {:encoding :json}))
  (add-tap (partial send! {:encoding :edn})))
