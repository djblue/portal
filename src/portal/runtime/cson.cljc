(ns ^:no-doc portal.runtime.cson
  "Clojure/Script Object Notation"
  (:refer-clojure :exclude [read])
  #?(:cljs (:require [goog.crypt.base64 :as Base64]
                     [portal.runtime.json-buffer :as json]
                     [portal.runtime.macros :as m])
     :clj  (:require [portal.runtime.json-buffer :as json]))
  #?(:clj  (:import [java.net URL]
                    [java.util Base64 Date UUID])))

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

#?(:clj
   (defn- box-long [buffer value]
     (let [js-min-int -9007199254740991
           js-max-int  9007199254740991]
       (if (<= js-min-int value js-max-int)
         (json/push-long buffer value)
         (-> buffer
             (json/push-string "long")
             (json/push-string (Long/toString value)))))))

#?(:clj (extend-type Byte    ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Short   ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Integer ToJson (-to-json [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Long    ToJson (-to-json [value buffer] (box-long buffer value))))
#?(:clj (extend-type Float   ToJson (-to-json [value buffer] (json/push-double buffer value))))
#?(:clj (extend-type Double  ToJson (-to-json [value buffer] (json/push-double buffer value))))

#?(:cljs (extend-type number ToJson (-to-json [value buffer] (json/push-double buffer value))))

(extend-type #?(:clj  String
                :cljs string)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "s")
        (json/push-string value))))

(extend-type #?(:clj  Boolean
                :cljs boolean)
  ToJson
  (-to-json [value buffer] (json/push-bool buffer value)))

(extend-type nil ToJson (-to-json [_value buffer] (json/push-null buffer)))

(defrecord Tagged [tag rep]
  ToJson
  (-to-json [_ buffer]
    (-to-json rep (json/push-string buffer tag))))

#?(:clj
   (defmethod print-method Tagged [v ^java.io.Writer w]
     (.write w ^String (:rep v)))
   :cljs
   (extend-type Tagged
     IPrintWithWriter
     (-pr-writer [this writer _opts]
       (-write writer (:rep this)))))

(defn tagged-value [tag rep] (assert tag string?) (->Tagged tag rep))

(defn tagged-value? [x] (instance? Tagged x))

(defn- ->meta [buffer]
  (let [m (->value buffer)]
    (with-meta (->value buffer) m)))

(defn- tagged-meta [buffer value]
  (if-let [m (meta value)]
    (-to-json m (json/push-string buffer "^"))
    buffer))

(defn- base64-encode [byte-array]
  #?(:clj  (.encodeToString (Base64/getEncoder) byte-array)
     :cljs (Base64/encodeByteArray byte-array)))

(defn- base64-decode [^String string]
  #?(:clj  (.decode (Base64/getDecoder) string)
     :cljs (Base64/decodeStringToUint8Array string)))

(extend-type #?(:clj  (Class/forName "[B")
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
     :cljs (js/BigInt (json/next-string buffer))))

#?(:clj
   (extend-type Character
     ToJson
     (-to-json [value buffer]
       (-> buffer
           (json/push-string "C")
           (json/push-long (int value))))))

(defn- ->char [buffer] (char (json/next-long buffer)))

#?(:bb (defn inst-ms [inst] (.getTime inst)))

(extend-type #?(:clj  Date
                :cljs js/Date)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "inst")
        (json/push-long (inst-ms value)))))

(defn- ->inst [buffer]
  #?(:clj  (Date. ^long (json/next-long buffer))
     :cljs (js/Date. (json/next-long buffer))))

(extend-type #?(:clj  UUID
                :cljs cljs.core/UUID)
  ToJson
  (-to-json [value buffer]
    (-> buffer
        (json/push-string "uuid")
        (json/push-string (str value)))))

(defn- ->uuid [buffer]
  #?(:clj  (UUID/fromString (json/next-string buffer))
     :cljs (uuid (json/next-string buffer))))

#?(:clj
   (extend-type URL
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
     :cljs (js/URL. (json/next-string buffer))))

(extend-type #?(:clj  clojure.lang.Keyword
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

(extend-type #?(:clj  clojure.lang.Cons
                :cljs cljs.core/Cons)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentList$EmptyList
                :cljs cljs.core/EmptyList)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.LazySeq
                :cljs cljs.core/LazySeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.ArraySeq
                :cljs cljs.core/IndexedSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$KeySeq
                :cljs cljs.core/KeySeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$ValSeq
                :cljs cljs.core/ValSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:clj
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
                :cljs cljs.core/Range)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" (into [] value))))

(extend-type #?(:clj  clojure.lang.Repeat
                :cljs cljs.core/Repeat)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentList
                :cljs cljs.core/List)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

(extend-type #?(:clj  clojure.lang.PersistentQueue
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
                :cljs cljs.core/ChunkedSeq)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "(" value)))

#?(:cljs
   (extend-type cljs.core/PersistentQueueSeq
     ToJson
     (-to-json [value buffer] (tagged-coll buffer "(" value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap$Seq
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
                :cljs cljs.core/PersistentVector)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "[" value)))

(extend-type #?(:clj  clojure.lang.APersistentVector$SubVector
                :cljs cljs.core/Subvec)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "[" value)))

(extend-type #?(:clj clojure.lang.MapEntry
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
                :cljs cljs.core/PersistentHashSet)
  ToJson
  (-to-json [value buffer] (tagged-coll buffer "#" value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeSet
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
                :cljs cljs.core/PersistentHashMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeMap
                :cljs cljs.core/PersistentTreeMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer "smap" value)))

#?(:clj
   (extend-type clojure.lang.APersistentMap
     ToJson
     (-to-json [value buffer] (tagged-map buffer value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap
                :cljs cljs.core/PersistentArrayMap)
  ToJson
  (-to-json [value buffer] (tagged-map buffer value)))

(extend-type #?(:clj  clojure.lang.IRecord
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

(defn- ->long [buffer]
  #?(:clj  (Long/parseLong (json/next-string buffer))
     :cljs (tagged-value "long" (json/next-string buffer))))

#?(:bb (def clojure.lang.TaggedLiteral (type (tagged-literal 'a :a))))

(extend-type #?(:clj  clojure.lang.TaggedLiteral
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
       (#?@(:bb [case] :clj [condp eq] :cljs [case])
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
        "N"    (->bigint buffer)
        "C"    (->char buffer)
        "bin"  (->bin buffer)
        "inst" (->inst buffer)
        "smap" (->sorted-map buffer)
        "sset" (->sset buffer)
        "url"  (->url buffer)
        "uuid" (->uuid buffer)
        "tag"  (->tagged-literal buffer)
        "long" (->long buffer)
        ((:default-handler *options*) op (->value buffer)))))))

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
