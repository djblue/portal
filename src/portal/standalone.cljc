(ns portal.standalone
  (:require
   [babashka.deps :as deps]
   [cheshire.core :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.main :as main]
   [cognitect.transit :as transit]
   [org.httpkit.server :as http]
   [portal.api :as p])
  (:import [java.io PushbackReader]))

(def port (or (edn/read-string (first *command-line-args*)) 53755))

(def cors-answer
  {:status 204
   :headers
   {"Access-Control-Allow-Origin" "*"
    "Access-Control-Allow-Headers" "origin, content-type"
    "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
    "Access-Control-Max-Age" 86400}})

(defn handler [request]
  (let [body (:body request)]
    (if-let [content-type (get-in request [:headers "content-type"])]
      (do (tap>
           (case content-type
             "application/transit+json" (transit/read (transit/reader body :json))
             "application/json"         (json/parse-stream (io/reader body) true)
             "application/edn"          (edn/read (PushbackReader. (io/reader body)))))
          {:status 200})
      cors-answer)))

(def server (http/run-server
             #'handler
             {:port                 port
              :legacy-return-value? false}))

(println "Server running on port:" (http/server-port server))

(p/open)

(.addShutdownHook
 (Runtime/getRuntime)
 (Thread. (fn [] (p/close))))

(add-tap #'p/submit)

(main/repl)
