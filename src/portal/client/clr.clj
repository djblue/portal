(ns ^:no-doc ^:no-check portal.client.clr
  (:require [portal.runtime]
            [portal.runtime.cson :as cson])
  (:import (System.Net.Http
            HttpClient
            HttpMethod
            HttpRequestMessage
            StringContent)
           (System.Text Encoding)))

(defn- serialize [encoding value]
  (try
    (case encoding
      :edn (binding [*print-meta* true]
             (pr-str value))
      :cson (cson/write value))
    (catch Exception ex
      (serialize encoding (Throwable->map ex)))))

(def client (HttpClient.))

(defn submit
  {:added "0.36.0" :see-also ["portal.api/submit"]}
  ([value] (submit nil value))
  ([{:keys [encoding port host]
     :or   {encoding :edn
            host     "localhost"
            port     53755}}
    value]
   (let [request (HttpRequestMessage.
                  HttpMethod/Post
                  (str "http://" host ":" port "/submit"))]
     (set! (.-Content request)
           (StringContent.
            (serialize encoding value)
            Encoding/UTF8
            (case encoding
              :edn     "application/edn"
              :cson    "application/cson")))
     (.EnsureSuccessStatusCode (.Send ^HttpClient client request)))))
