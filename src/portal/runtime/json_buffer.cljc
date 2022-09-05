(ns portal.runtime.json-buffer
  #?(:bb  (:require [portal.runtime.json :as json])
     :clj (:import (com.google.gson.stream JsonReader JsonToken JsonWriter)
                   (java.io StringReader StringWriter))))

(defprotocol IShift (-shift [this]))

(defn ->reader [data]
  #?(:bb
     (let [source (into [] (json/read data))
           n      (volatile! 0)]
       (reify IShift
         (-shift [this]
           (let [result (nth source @n)]
             (vswap! n unchecked-inc)
             result))))
     :clj
     (doto (JsonReader. (StringReader. data))
       (.beginArray))
     :cljs
     (let [source (.parse js/JSON data)
           n      (volatile! 0)]
       (reify IShift
         (-shift [_this]
           (let [result (aget source @n)]
             (vswap! n unchecked-inc)
             result))))))

(defn push-null   [buffer]
  #?(:bb   (conj! buffer nil)
     :clj  (doto ^JsonWriter buffer (.nullValue))
     :cljs (doto ^js buffer (.push nil))))

(defn push-bool   [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Boolean value))
     :cljs (doto ^js buffer (.push value))))

(defn push-long   [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Long value))
     :cljs (doto ^js buffer (.push value))))

(defn push-double [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Double value))
     :cljs (doto ^js buffer (.push value))))

(defn push-string [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^String value))
     :cljs (doto ^js buffer (.push value))))

(defn push-value [buffer value]
  (cond
    (nil? value)     (push-null buffer)
    (boolean? value) (push-bool buffer value)
    (int? value)     (push-long buffer value)
    (double? value)  (push-double buffer value)
    (string? value)  (push-string buffer value)))

(defn next-null [buffer]
  #?(:bb   (-shift buffer)
     :clj  (.nextNull ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-bool ^Boolean [buffer]
  #?(:bb   (-shift buffer)
     :clj  (.nextBoolean ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-long ^long [buffer]
  #?(:bb   (-shift buffer)
     :clj  (.nextLong ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-double ^double [buffer]
  #?(:bb   (-shift buffer)
     :clj  (.nextDouble ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-string ^String [buffer]
  #?(:bb   (-shift buffer)
     :clj  (.nextString ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-value [buffer]
  #?(:bb (-shift buffer)
     :clj
     (condp identical? (.peek ^JsonReader buffer)
       JsonToken/STRING  (next-string ^JsonReader buffer)
       JsonToken/BOOLEAN (next-bool ^JsonReader buffer)
       JsonToken/NULL    (next-null ^JsonReader buffer)
       JsonToken/NUMBER  (let [n (next-string buffer)]
                           (if (== (.indexOf n 46) -1)
                             (Long/parseLong n)
                             (Double/parseDouble n))))
     :cljs (-shift buffer)))

(defn with-buffer [f value]
  #?(:bb   (json/write (persistent! (f (transient []) value)))
     :cljs (.stringify js/JSON (f (js/Array.) value))
     :clj  (let [out  (StringWriter.)
                 json (JsonWriter. out)]
             (.beginArray json)
             (f json value)
             (.endArray json)
             (.close json)
             (.toString out))))
