(ns portal.runtime.transit
  (:require [cognitect.transit :as transit]
            [io.aviso.exception :as ex]
            [portal.runtime :as rt])
  (:import [java.io ByteArrayOutputStream]))

(defn- var->symbol [v]
  (let [m (meta v)]
    (with-meta (symbol (str (:ns m)) (str (:name m))) m)))

(defn edn->json-stream [value out]
  (let [writer
        (transit/writer
         out
         :json
         {:handlers
          {clojure.lang.Var
           (transit/write-handler "portal.transit/var" var->symbol)
           java.net.URL
           (transit/write-handler "r" str)
           java.lang.Throwable
           (transit/write-handler "portal.transit/exception" #(ex/analyze-exception % nil))}
          :transform transit/write-meta
          :default-handler
          (transit/write-handler
           "portal.transit/object"
           (fn [o]
             {:id (rt/instance->uuid o)
              :meta (when (instance? clojure.lang.IObj o)
                      (meta o))
              :type (pr-str (type o))
              :string (pr-str o)}))})]
    (transit/write writer value)
    (.toString out)))

(defn edn->json [value]
  (let [out (ByteArrayOutputStream. (* 10 1024 1024))]
    (edn->json-stream value out)
    (.toString out)))

(defn json-stream->edn [in]
  (transit/read
   (transit/reader
    in
    :json
    {:handlers
     {"portal.transit/var" (transit/read-handler find-var)
      "portal.transit/object" (transit/read-handler (comp rt/uuid->instance :id))}})))
