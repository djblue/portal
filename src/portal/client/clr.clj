(ns ^:no-doc ^:no-check portal.client.clr
  (:require [portal.runtime]
            [portal.runtime.cson :as cson])
  (:import (System.Collections.Generic KeyValuePair)
           (System.Net.Http
            HttpClient
            HttpCompletionOption
            HttpMethod
            HttpRequestMessage
            HttpResponseMessage
            StringContent)
           (System.Net.Http.Headers HttpResponseHeaders)
           (System.Text Encoding)))

(def client (HttpClient.))

(defn- ->headers [^HttpResponseMessage response]
  (let [^HttpResponseHeaders headers         (.Headers response)
        ^HttpResponseHeaders content-headers (.Headers (.Content response))]
    (persistent!
     (reduce
      (fn [out ^KeyValuePair x]
        (let [k (.Key x) v (.Value x)]
          (assoc! out k (cond-> v (= 1 (count v)) first))))
      (transient {})
      (concat
       (iterator-seq (.GetEnumerator headers))
       (iterator-seq (.GetEnumerator content-headers)))))))

(defn- ->response [^HttpResponseMessage response]
  {:status  (int (.StatusCode response))
   :headers (->headers response)
   :body    (.Content response)})

(defn request [{:keys [url method body headers]
                :or   {method :get}}]
  (let [request (HttpRequestMessage.
                 (case method
                   :get HttpMethod/Get
                   :post HttpMethod/Post
                   :put HttpMethod/Put
                   :delete HttpMethod/Delete)
                 url)]
    (when body
      (set! (.-Content request)
            (StringContent.
             body
             Encoding/UTF8
             (get headers "content-type" "text/plain"))))
    (->response (.Send ^HttpClient client request HttpCompletionOption/ResponseContentRead))))

(defn- serialize [encoding value]
  (try
    (case encoding
      :edn (binding [*print-meta* true]
             (pr-str value))
      :cson (cson/write value))
    (catch Exception ex
      (serialize encoding (Throwable->map ex)))))

(defn submit
  {:added "0.36.0" :see-also ["portal.api/submit"]}
  ([value] (submit nil value))
  ([{:keys [encoding port host]
     :or   {encoding :edn
            host     "localhost"
            port     53755}}
    value]
   (request
    {:url (str "http://" host ":" port "/submit")
     :method :post
     :headers
     {"content-type"
      (case encoding
        :edn     "application/edn"
        :cson    "application/cson")}
     :body (serialize encoding value)})))
