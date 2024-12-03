(ns portal.runtime.json-buffer
  {:no-doc true}
  (:require
   #?(:bb [portal.runtime.json :as json]
      :cljr [portal.runtime.clr.assembly]))
  (:import
   #?@(:clj
       [(com.google.gson.stream JsonReader JsonToken JsonWriter)
        (java.io StringReader StringWriter)]

       :cljr
       [(System.Text.Json JsonDocumentOptions JsonElement JsonSerializerOptions JsonValueKind)
        (System.Text.Json.Nodes JsonArray JsonNode JsonNodeOptions JsonValue)])))

(defn -shift [this] (this))

#?(:cljs (defn shifter [source]
           (let [this #js {:n 0}]
             (fn []
               (let [n      (.-n this)
                     result (aget source n)]
                 (set! (.-n this) (unchecked-inc n))
                 result)))))

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
     (shifter (.parse js/JSON data))))

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

(defn next-string ^String [buffer]
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
