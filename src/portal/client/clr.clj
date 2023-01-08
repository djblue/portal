(ns ^:no-check portal.client.clr
  (:import (System.Net.Http
            HttpClient
            HttpMethod
            HttpRequestMessage
            StringContent)
           (System.Text Encoding)))

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
            (case encoding
              :edn (binding [*print-meta* true]
                     (pr-str value)))
            Encoding/UTF8
            (case encoding
              :edn     "application/edn")))
     (.Send ^HttpClient client request))))
