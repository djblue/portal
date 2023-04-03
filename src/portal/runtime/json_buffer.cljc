(ns ^:no-doc portal.runtime.json-buffer
  #?(:cljr (:require [portal.runtime.clr.assembly]))
  #?(:bb  (:require [portal.runtime.json :as json])
     :clj (:import (com.google.gson.stream JsonReader JsonToken JsonWriter)
                   (java.io StringReader StringWriter))
     :cljr (:import (System.Text.Json
                     JsonElement
                     JsonValueKind
                     JsonDocumentOptions
                     JsonSerializerOptions)
                    (System.Text.Json.Nodes
                     JsonNode
                     JsonArray
                     JsonValue
                     JsonNodeOptions))))

(defprotocol IShift (-shift [this]))

(defn ->reader [data]
  #?(:bb
     (volatile! (json/read data))
     :cljr
     (volatile!
      (seq
       (JsonNode/Parse ^String data
                       (JsonNodeOptions.)
                       (JsonDocumentOptions.))))
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
     :cljr (doto ^JsonArray buffer (.Add nil))
     :cljs (doto ^js buffer (.push nil))))

(defn push-bool   [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Boolean value))
     :cljr (doto ^JsonArray buffer (.Add value))
     :cljs (doto ^js buffer (.push value))))

(defn push-long   [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Long value))
     :cljr (doto ^JsonArray buffer (.Add value))
     :cljs (doto ^js buffer (.push value))))

(defn push-double [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^Double value))
     :cljr (doto ^JsonArray buffer (.Add value))
     :cljs (doto ^js buffer (.push value))))

(defn push-string [buffer value]
  #?(:bb   (conj! buffer value)
     :clj  (doto ^JsonWriter buffer (.value ^String value))
     :cljr (doto ^JsonArray buffer (.Add value))
     :cljs (doto ^js buffer (.push value))))

(defn push-value [buffer value]
  (cond
    (nil? value)     (push-null buffer)
    (boolean? value) (push-bool buffer value)
    (int? value)     (push-long buffer value)
    (double? value)  (push-double buffer value)
    (string? value)  (push-string buffer value)))

(defn next-null [buffer]
  #?(:bb   (let [v (first @buffer)] (vswap! buffer rest) v)
     :clj  (.nextNull ^JsonReader buffer)
     :cljr (do (vswap! buffer rest) nil)
     :cljs (-shift buffer)))

(defn next-bool [buffer]
  #?(:bb   (let [v (first @buffer)] (vswap! buffer rest) v)
     :cljr (let [v ^JsonValue (first @buffer)]
             (vswap! buffer rest)
             (.GetValue v (type-args System.Boolean)))
     :clj  (.nextBoolean ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-long ^long [buffer]
  #?(:bb   (let [v (first @buffer)] (vswap! buffer rest) v)
     :cljr (let [v ^JsonValue (first @buffer)]
             (vswap! buffer rest)
             (.GetValue v (type-args System.Int64)))
     :clj  (.nextLong ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-double ^double [buffer]
  #?(:bb   (let [v (first @buffer)] (vswap! buffer rest) v)
     :cljr (let [v ^JsonValue (first @buffer)]
             (vswap! buffer rest)
             (.GetValue v (type-args System.Double)))
     :clj  (.nextDouble ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-string [buffer]
  #?(:bb   (let [v (first @buffer)] (vswap! buffer rest) v)
     :cljr (let [v ^JsonValue (first @buffer)]
             (vswap! buffer rest)
             (.GetValue v (type-args System.String)))
     :clj  (.nextString ^JsonReader buffer)
     :cljs (-shift buffer)))

(defn next-value [buffer]
  #?(:bb (let [v (first @buffer)] (vswap! buffer rest) v)
     :cljr
     (let [result
           (when-let [^JsonElement value
                      (some-> ^JsonValue (first @buffer)
                              (.GetValue (type-args JsonElement)))]
             (condp identical? (.ValueKind value)
               JsonValueKind/String (.GetString value)
               JsonValueKind/False  false
               JsonValueKind/True   true
               JsonValueKind/Null   nil
               JsonValueKind/Number (let [n (.ToString value)]
                                      (if (.Contains n \.)
                                        (.GetDouble value)
                                        (.GetInt64 value)))))]
       (vswap! buffer rest)
       result)
     :clj
     (condp identical? (.peek ^JsonReader buffer)
       JsonToken/STRING  (next-string ^JsonReader buffer)
       JsonToken/BOOLEAN (next-bool ^JsonReader buffer)
       JsonToken/NULL    (next-null ^JsonReader buffer)
       JsonToken/NUMBER  (let [n ^String (next-string buffer)]
                           (if (== (.indexOf n 46) -1)
                             (Long/parseLong n)
                             (Double/parseDouble n))))
     :cljs (-shift buffer)))

(defn with-buffer [f value]
  #?(:bb   (json/write (persistent! (f (transient []) value)))
     :cljs (.stringify js/JSON (f (js/Array.) value))
     :cljr (let [json (JsonArray. (JsonNodeOptions.))]
             (f json value)
             (.ToJsonString json (JsonSerializerOptions.)))
     :clj  (let [out  (StringWriter.)
                 json (JsonWriter. out)]
             (.beginArray json)
             (f json value)
             (.endArray json)
             (.close json)
             (.toString out))))
