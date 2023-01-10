(ns ^:no-doc portal.runtime.cson
  "Clojure/Script Object Notation"
  (:refer-clojure :exclude [read])
  #?(:cljs (:require [goog.crypt.base64 :as Base64]
                     [portal.runtime.json-buffer :as json]
                     [portal.runtime.macros :as m])
     :cljr (:require [portal.runtime.json-buffer :as json])
     :clj  (:require [portal.runtime.json-buffer :as json]))
  #?(:clj  (:import [java.net URL]
                    [java.util Base64 Date UUID])
     :cljs (:import [goog.math Long])))

(defprotocol ToJson (-to-json [value buffer]))

(declare ->value)

(defonce ^:dynamic *options* nil)

(defn- transform [value]
  (if-let [f (:transform *options*)]
    (f value)
    value))

(defn- to-json [buffer value] (-to-json (transform value) buffer))

(defn tag [buffer tag value]
  (assert tag string?)
  (-to-json value (json/push-string buffer tag)))

(defn- box-long [buffer value]
  #?(:cljr
     (let [js-min-int -9007199254740991
           js-max-int  9007199254740991]
       (if (<= js-min-int value js-max-int)
         (json/push-long buffer value)
         (-> buffer
             (json/push-string "long")
             (json/push-string (.ToString ^System.Int64 value)))))
     :clj
     (let [js-min-int -9007199254740991
           js-max-int  9007199254740991]
       (if (<= js-min-int value js-max-int)
         (json/push-long buffer value)
         (-> buffer
             (json/push-string "long")
             (json/push-string (Long/toString value)))))
     :cljs
     (-> buffer
         (json/push-string "long")
         (json/push-string (str value)))))

(extend-type #?(:cljr System.Int64
                :clj  Long
                :cljs Long)
  ToJson
  (-to-json [value buffer] (box-long buffer value)))

(defn- ->long [buffer]
  #?(:clj  (Long/parseLong (json/next-string buffer))
     :cljr (System.Int64/Parse (json/next-string buffer))
     :cljs (.fromString Long (json/next-string buffer))))

(defn is-finite? [value]
  #?(:clj  (Double/isFinite ^Double value)
     :cljr (Double/IsFinite ^Double value)
     :cljs (.isFinite js/Number value)))

(defn nan? [value]
  #?(:clj  (.equals ^Double value ##NaN)
     :cljr (Double/IsNaN value)
     :cljs (.isNaN js/Number value)))

(defn inf? [value]
  #?(:clj  (.equals ^Double value ##Inf)
     :cljr (Double/IsInfinity value)
     :cljs (== ##Inf value)))

(defn -inf? [value]
  #?(:clj  (.equals ^Double value ##-Inf)
     :cljr (Double/IsNegativeInfinity value)
     :cljs (== ##-Inf value)))

(defn- push-double [buffer value]
  (cond
    (is-finite? value) #?(:cljr    (if-not (zero? (mod value 1))
                                     (json/push-double buffer value)
                                     (-> buffer
                                         (json/push-string "D")
                                         (json/push-double value)))
                          :default (json/push-double buffer value))
    (nan? value)       (json/push-string buffer "nan")
    (inf? value)       (json/push-string buffer "inf")
    (-inf? value)      (json/push-string buffer "-inf")))

(defn ->double [buffer]
  (double (json/next-double buffer)))

#?(:clj (extend-type Byte    ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Short   ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Integer ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Float   ToJson (-to-json [value buffer] (push-double buffer value))))
#?(:clj (extend-type Double  ToJson (-to-json [value buffer] (push-double buffer value))))

#?(:cljr (extend System.Byte  ToJson {:-to-json (fn [value buffer] (json/push-long buffer value))}))
#?(:cljr (extend System.Int16 ToJson {:-to-json (fn [value buffer] (json/push-long buffer value))}))
#?(:cljr (extend System.Int32 ToJson {:-to-json (fn [value buffer] (json/push-long buffer value))}))

#?(:cljr (extend-type System.Double  ToJson (-to-json [value buffer] (push-double buffer value))))

#?(:cljs (extend-type number ToJson (-to-json [value buffer] (push-double buffer value))))

#?(:clj
   (extend-type clojure.lang.Ratio
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (numerator value))
           (json/push-long (denominator value)))))
   :cljs
   (deftype Ratio [numerator denominator]
     ToJson
     (-to-json [_ buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long numerator)
           (json/push-long denominator)))
     IPrintWithWriter
     (-pr-writer [_this writer _opts]
       (-write writer (str numerator))
       (-write writer "/")
       (-write writer (str denominator)))))

(defn ->ratio [buffer]
  (let [n (json/next-long buffer)
        d (json/next-long buffer)]
    #?(:clj (/ n d) :cljs (Ratio. n d))))

(extend-type #?(:cljr System.String
                :clj  String
                :cljs string)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "s")
        (json/push-string value))))

#?(:cljr
   (extend System.Boolean
     ToJson
     {:-to-json (fn [value buffer] (json/push-bool buffer value))})
   :clj
   (extend-type Boolean
     ToJson
     (-to-json [value buffer] (json/push-bool buffer value)))
   :cljs
   (extend-type boolean
     ToJson
     (-to-json [value buffer] (json/push-bool buffer value))))

(extend-type nil ToJson (-to-json [_value buffer] (json/push-null buffer)))

(defn- can-meta? [value]
  #?(:clj  (instance? clojure.lang.IObj value)
     :cljr (instance? clojure.lang.IObj value)
     :cljs (implements? IMeta value)))

(defn- ->meta [buffer]
  (let [m (->value buffer) v (->value buffer)]
    (if-not (can-meta? v) v (with-meta v m))))

(defn- tagged-meta [buffer value]
  (if-let [m (meta value)]
    (-to-json m (json/push-string buffer "^"))
    buffer))

(defn- bb-fix
  "Remove bb type tag for records which cause an infinite loop."
  [tagged]
  #?(:bb (vary-meta tagged dissoc :type) :default tagged))

(defrecord Tagged [tag rep]
  ToJson
  (-to-json [this buffer]
    (-to-json (:rep this)
              (-> buffer
                  (tagged-meta (bb-fix this))
                  (json/push-string (:tag this))))))

#?(:clj
   (defmethod print-method Tagged [v ^java.io.Writer w]
     (.write w "#")
     (.write w ^String (:tag v))
     (.write w " ")
     (.write w (pr-str (:rep v))))
   :cljs
   (extend-type Tagged
     IPrintWithWriter
     (-pr-writer [this writer _opts]
       (-write writer "#")
       (-write writer (:tag this))
       (-write writer " ")
       (-write writer (pr-str (:rep this))))))

(defn tagged-value [tag rep] {:pre [(string? tag)]} (->Tagged tag rep))

(defn tagged-value? [x] (instance? Tagged x))

(defn base64-encode [byte-array]
  #?(:clj  (.encodeToString (Base64/getEncoder) byte-array)
     :cljs (Base64/encodeByteArray byte-array)
     :cljr (Convert/ToBase64String byte-array)))

(defn base64-decode [string]
  #?(:clj  (.decode (Base64/getDecoder) ^String string)
     :cljs (Base64/decodeStringToUint8Array string)
     :cljr (Convert/FromBase64String string)))

(extend-type #?(:clj  (Class/forName "[B")
                :cljr (Type/GetType "System.Byte[]")
                :cljs js/Uint8Array)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "bin")
        (json/push-string (base64-encode value)))))

(defn- ->bin [buffer] (base64-decode (json/next-string buffer)))

#?(:clj
   (extend-type clojure.lang.BigInt
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "N")
           (json/push-string (str value)))))
   :cljr
   (extend-type clojure.lang.BigInt
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "N")
           (json/push-string (str value))))))

#?(:cljs
   (m/extend-type?
    js/BigInt
    ToJson
    (-to-json [value buffer]
              (-> buffer
                  (json/push-string "N")
                  (json/push-string (str value))))))

(defn- ->bigint [buffer]
  #?(:clj  (bigint    (json/next-string buffer))
     :cljr (bigint    (json/next-string buffer))
     :cljs (js/BigInt (json/next-string buffer))))

#?(:clj
   (extend-type Character
     ToJson
     (-to-json [value buffer]
       (tag buffer "C" (int value))))
   :cljr
   (extend System.Char
     ToJson
     {:-to-json (fn [value buffer]
                  (tag buffer "C" (int value)))})
   :cljs
   (deftype Character [code]
     ToJson
     (-to-json [_this buffer]
       (tag buffer "C" code))
     IHash
     (-hash [_this] code)
     IEquiv
     (-equiv [_this other]
       (and (instance? Character other)
            (== code (.-code other))))
     IPrintWithWriter
     (-pr-writer [_this writer _opts]
       (-write writer "\\")
       (-write writer
               (case code
                 10 "newline"
                 32 "space"
                 9  "tab"
                 8  "backspace"
                 12 "formfeed"
                 13 "return"
                 (.fromCharCode js/String code))))))

(defn- ->char [buffer]
  #?(:clj  (char (->value buffer))
     :cljr (char (->value buffer))
     :cljs (Character. (->value buffer))))

#?(:bb (defn inst-ms [inst] (.getTime inst)))

(extend-type #?(:clj  Date
                :cljr System.DateTime
                :cljs js/Date)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "inst")
        (json/push-long (inst-ms value)))))

(defn- ->inst [buffer]
  #?(:clj  (Date. ^long (json/next-long buffer))
     :cljr (.DateTime
            (System.DateTimeOffset/FromUnixTimeMilliseconds
             (json/next-long buffer)))
     :cljs (js/Date. (json/next-long buffer))))

(extend-type #?(:clj  UUID
                :cljr System.Guid
                :cljs cljs.core/UUID)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "uuid")
        (json/push-string (str value)))))

(defn- ->uuid [buffer]
  #?(:clj  (UUID/fromString (json/next-string buffer))
     :cljr (System.Guid/Parse (json/next-string buffer))
     :cljs (uuid (json/next-string buffer))))

#?(:clj
   (extend-type URL
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "url")
           (json/push-string (str value)))))
   :cljr
   (extend-type System.Uri
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "url")
           (json/push-string (str value))))))

#?(:cljs
   (m/extend-type?
    js/URL
    ToJson
    (-to-json [value buffer]
              (-> buffer
                  (json/push-string "url")
                  (json/push-string (str value))))))

(defn- ->url [buffer]
  #?(:clj  (URL. (json/next-string buffer))
     :cljr (System.Uri. (json/next-string buffer))
     :cljs (js/URL. (json/next-string buffer))))

(extend-type #?(:clj  clojure.lang.Keyword
                :cljr clojure.lang.Keyword
                :cljs cljs.core/Keyword)

  ToJson
  (-to-json [value buffer]
    (if-let [ns (namespace value)]
      (-> buffer
          (json/push-string ";")
          (json/push-string ns)
          (json/push-string (name value)))
      (-> buffer
          (json/push-string ":")
          (json/push-string (name value))))))

(defn- ->keyword [buffer]
  (keyword (json/next-string buffer)))

(defn- ->keyword-2 [buffer]
  (keyword (json/next-string buffer) (json/next-string buffer)))

(extend-type #?(:clj  clojure.lang.Symbol
                :cljr clojure.lang.Symbol
                :cljs cljs.core/Symbol)
  ToJson
  (-to-json [value buffer]
    (if-let [ns (namespace value)]
      (-> buffer
          (tagged-meta value)
          (json/push-string "%")
          (json/push-string ns)
          (json/push-string (name value)))
      (-> buffer
          (tagged-meta value)
          (json/push-string "$")
          (json/push-string (name value))))))

(defn- ->symbol [buffer]
  (symbol (json/next-string buffer)))

(defn- ->symbol-2 [buffer]
  (symbol (json/next-string buffer) (json/next-string buffer)))

(defn- tagged-coll [buffer tag value]
  (reduce
   to-json
   (-> buffer
       (tagged-meta value)
       (json/push-string tag)
       (json/push-long (count value)))
   value))

#?(:bb (def clojure.lang.APersistentMap$KeySeq (type (keys {0 0}))))
#?(:bb (def clojure.lang.APersistentMap$ValSeq (type (vals {0 0}))))
#?(:bb (def clojure.lang.APersistentVector$SubVector (type (subvec [1 2] 0 1))))
#?(:bb (def clojure.lang.ArraySeq ((fn [& args] (type args)) 1)))
#?(:bb (def clojure.lang.LongRange (type (range 1))))
#?(:bb (def clojure.lang.PersistentArrayMap$Seq (type (seq {0 0}))))
#?(:bb (def clojure.lang.PersistentList$EmptyList (type (list))))
#?(:bb (def clojure.lang.PersistentVector$ChunkedSeq (type (seq [1]))))
#?(:bb (def clojure.lang.Range (type (range 1.0))))
#?(:bb (def clojure.lang.StringSeq (type (seq "hi"))))

#?(:clj
   (extend-type clojure.lang.StringSeq
     ToJson
     (-to-json [value buffer]
       (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.Cons
                :cljr clojure.lang.Cons
                :cljs cljs.core/Cons)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentList$EmptyList
                :cljr clojure.lang.PersistentList+EmptyList
                :cljs cljs.core/EmptyList)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.LazySeq
                :cljr clojure.lang.LazySeq
                :cljs cljs.core/LazySeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.ArraySeq
                :cljr clojure.lang.ArraySeq
                :cljs cljs.core/IndexedSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$KeySeq
                :cljr clojure.lang.APersistentMap+KeySeq
                :cljs cljs.core/KeySeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$ValSeq
                :cljr clojure.lang.APersistentMap+ValSeq
                :cljs cljs.core/ValSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:clj
   (extend-type clojure.lang.LongRange
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value)))
   :cljr
   (extend-type clojure.lang.LongRange
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

#?(:cljs
   (m/extend-type?
    ^:cljs.analyzer/no-resolve
    cljs.core/IntegerRange
    ToJson
    (-to-json [value buffer] (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.Range
                :cljr clojure.lang.Range
                :cljs cljs.core/Range)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" (into [] value))))

(extend-type #?(:clj  clojure.lang.Repeat
                :cljr clojure.lang.Repeat
                :cljs cljs.core/Repeat)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentList
                :cljr clojure.lang.PersistentList
                :cljs cljs.core/List)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentQueue
                :cljr clojure.lang.PersistentQueue
                :cljs cljs.core/PersistentQueue)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:cljs
   (extend-type cljs.core/RSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

#?(:bb nil
   :clj
   (extend-type clojure.lang.ChunkedCons
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value)))
   :cljs
   (extend-type cljs.core/ChunkedCons
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.PersistentVector$ChunkedSeq
                :cljr clojure.lang.PersistentVector+ChunkedSeq
                :cljs cljs.core/ChunkedSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:cljs
   (extend-type cljs.core/PersistentQueueSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap$Seq
                :cljr clojure.lang.PersistentArrayMap+Seq
                :cljs cljs.core/PersistentArrayMapSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:cljs
   (extend-type cljs.core/PersistentTreeMapSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

#?(:cljs
   (extend-type cljs.core/NodeSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

#?(:cljs
   (extend-type cljs.core/ArrayNodeSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.PersistentVector
                :cljr clojure.lang.PersistentVector
                :cljs cljs.core/PersistentVector)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "[" value)))

(extend-type #?(:clj  clojure.lang.APersistentVector$SubVector
                :cljr clojure.lang.APersistentVector+SubVector
                :cljs cljs.core/Subvec)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "[" value)))

(extend-type #?(:clj  clojure.lang.MapEntry
                :cljr clojure.lang.MapEntry
                :cljs cljs.core/MapEntry)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "[" (into [] value))))

(defn- ->into [zero buffer]
  (let [n (json/next-long buffer)]
    (loop [i 0 out (transient zero)]
      (if (== i n)
        (persistent! out)
        (recur
         (unchecked-inc i)
         (conj! out (->value buffer)))))))

(extend-type #?(:clj  clojure.lang.PersistentHashSet
                :cljr clojure.lang.PersistentHashSet
                :cljs cljs.core/PersistentHashSet)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "#" value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeSet
                :cljr clojure.lang.PersistentTreeSet
                :cljs cljs.core/PersistentTreeSet)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "sset" value)))

(defn- ->sset [buffer]
  (let [n      (json/next-long buffer)
        values (for [_ (range n)] (->value buffer))
        order  (zipmap values (range))]
    (into
     (sorted-set-by
      (fn [a b]
        (compare (get order a) (get order b))))
     values)))

(defn- tagged-map
  ([buffer value] (tagged-map buffer "{" value))
  ([buffer tag value]
   (reduce-kv
    (fn [buffer k v]
      (-> buffer
          (to-json k)
          (to-json v)))
    (-> buffer
        (tagged-meta value)
        (json/push-string tag)
        (json/push-long (count value)))
    value)))

(extend-type #?(:clj  clojure.lang.PersistentHashMap
                :cljr clojure.lang.PersistentHashMap
                :cljs cljs.core/PersistentHashMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeMap
                :cljr clojure.lang.PersistentTreeMap
                :cljs cljs.core/PersistentTreeMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer "smap" value)))

#?(:clj
   (extend-type clojure.lang.APersistentMap
     ToJson
     (-to-json [value buffer] (tagged-map buffer value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap
                :cljr clojure.lang.PersistentArrayMap
                :cljs cljs.core/PersistentArrayMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer value)))

(extend-type #?(:clj  clojure.lang.IRecord
                :cljr clojure.lang.IRecord
                :cljs cljs.core/IRecord)
  ToJson
  (-to-json [value buffer] (tagged-map buffer value)))

(defn- ->map [buffer]
  (let [n (json/next-long buffer)]
    (loop [i 0 m (transient {})]
      (if (== i n)
        (persistent! m)
        (recur
         (unchecked-inc i)
         (assoc! m (->value buffer) (->value buffer)))))))

(defn- ->sorted-map [buffer]
  (let [n      (json/next-long buffer)
        pairs  (for [_ (range n)]
                 [(->value buffer) (->value buffer)])
        order  (zipmap (map first pairs) (range))]
    (into
     (sorted-map-by
      (fn [a b]
        (compare (get order a) (get order b))))
     pairs)))

#?(:bb (def clojure.lang.TaggedLiteral (type (tagged-literal 'a :a))))

(extend-type #?(:clj  clojure.lang.TaggedLiteral
                :cljr clojure.lang.TaggedLiteral
                :cljs cljs.core/TaggedLiteral)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "tag")
        (json/push-string (name (:tag value)))
        (to-json (:form value)))))

(defn- ->tagged-literal [buffer]
  (tagged-literal (symbol (json/next-string buffer)) (->value buffer)))

#?(:clj (defn- eq ^Boolean [^String a b] (.equals a b)))

(defn- ->value [buffer]
  (let [op (json/next-value buffer)]
    (if-not (string? op)
      op
      (transform
       (#?@(:bb [case] :cljr [case] :clj [condp eq] :cljs [case])
        op
        "s"    (json/next-string buffer)
        ":"    (->keyword buffer)
        "{"    (->map buffer)
        "$"    (->symbol buffer)
        "["    (->into [] buffer)
        "("    (or (list* (->into [] buffer)) '())
        ";"    (->keyword-2 buffer)
        "%"    (->symbol-2 buffer)
        "#"    (->into #{} buffer)
        "^"    (->meta buffer)
        "D"    (->double buffer)
        "N"    (->bigint buffer)
        "C"    (->char buffer)
        "R"    (->ratio buffer)
        "bin"  (->bin buffer)
        "inst" (->inst buffer)
        "smap" (->sorted-map buffer)
        "sset" (->sset buffer)
        "url"  (->url buffer)
        "uuid" (->uuid buffer)
        "tag"  (->tagged-literal buffer)
        "long" (->long buffer)
        "nan"  ##NaN
        "inf"  ##Inf
        "-inf" ##-Inf
        (let [handler (:default-handler *options* tagged-value)]
          (handler op (->value buffer))))))))

(defn write
  ([value] (write value nil))
  ([value options]
   (binding [*options* options]
     (json/with-buffer to-json value))))

(defn read
  ([string] (read string nil))
  ([string options]
   (binding [*options* options]
     (->value (json/->reader string)))))
