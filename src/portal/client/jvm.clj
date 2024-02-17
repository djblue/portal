(ns portal.client.jvm
  (:require [org.httpkit.client :as http]
            [portal.runtime]
            [portal.runtime.cson :as cson]
            [portal.runtime.json :as json]
            [portal.runtime.transit :as transit]))

(defn- serialize [encoding value]
  (try
    (case encoding
      :json    (json/write value)
      :transit (transit/write value)
      :cson    (cson/write value)
      :edn     (binding [*print-meta* true]
                 (pr-str value)))
    (catch Exception ex
      (serialize encoding (Throwable->map ex)))))

(defn submit
  "Tap target function.

  Will submit values to a remote portal. Tapped value must be serialiable with
  the encoding method provided.

  Usage:

  ```clojure
  (def submit (partial p/submit {:port 5678})) ;; :encoding :edn is the default
  ;; (def submit (partial p/submit {:port 5678 :encoding :json}))
  ;; (def submit (partial p/submit {:port 5678 :encoding :transit}))

  (add-tap #'submit)
  (remove-tap #'submit)
  ```
  "
  {:added "0.18.0" :see-also ["portal.api/submit"]}
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
         :cson    "application/cson"
         :transit "application/transit+json"
         :edn     "application/edn")}
      :body (serialize encoding value)})))

(comment
  (submit {:runtime :jvm :value "hello jvm"})
  (submit {:port 1664} {:runtime :jvm :value "hello jvm"})

  (add-tap submit)
  (tap> {:runtime :jvm :value "hello jvm"})

  (add-tap (partial submit {:encoding :json}))
  (add-tap (partial submit {:encoding :transit})))
