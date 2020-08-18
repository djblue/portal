(ns portal.runtime.transit
  (:refer-clojure :exclude [find-var])
  (:require [cognitect.transit :as transit]
            [portal.runtime :as rt])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]))

(defn- var->symbol [v]
  (let [m (meta v)
        s (symbol (str (:ns m)) (str (:name m)))]
    (swap! rt/instance-cache assoc [:var s] v)
    (with-meta s m)))

(defn- find-var [s] (get @rt/instance-cache [:var s]))

(defn edn->json-stream [value out]
  (let [writer
        (transit/writer
         out
         :json
         {:handlers
          {(type #'var->symbol)
           (transit/write-handler "portal.transit/var" var->symbol)
           java.net.URL
           (transit/write-handler "r" str)}
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
    (transit/write writer value)))

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

(defn json->edn [^String json]
  (json-stream->edn (ByteArrayInputStream. (.getBytes json))))
