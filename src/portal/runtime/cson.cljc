(ns ^:no-doc portal.runtime.cson
  "Clojure/Script Object Notation"
  (:refer-clojure :exclude [read])
  #?(:clj  (:require [portal.runtime.json-buffer :as json])
     :cljr (:require [portal.runtime.json-buffer :as json])
     :joyride
     (:require
      [portal.runtime.json-buffer :as json]
      [portal.runtime.macros :as m])
     :org.babashka/nbb
     (:require
      [portal.runtime.json-buffer :as json]
      [portal.runtime.macros :as m])
     :cljs
     (:require
      [goog.crypt.base64 :as Base64]
      [portal.runtime.json-buffer :as json]
      [portal.runtime.macros :as m])
     :lpy
     (:require [portal.runtime.json-buffer :as json]))
  #?(:clj  (:import [java.net URL]
                    [java.util Base64 Date UUID])
     :joyride (:import)
     :org.babashka/nbb (:import)
     :cljs (:import [goog.math Long])
     :lpy  (:import [basilisp.lang :as lang]
                    [datetime :as datetime]
                    [fractions :as fractions]
                    [math :as math]
                    [uuid :as uuid])))

(defprotocol ToJson (to-json* [value buffer]))

(declare ->value)

(defonce ^:dynamic *options* nil)

(defn- transform [value]
  (if-let [f (:transform *options*)]
    (f value)
    value))

(defn- to-json [buffer value] (to-json* (transform value) buffer))

(defn tag [buffer tag value]
  (assert tag string?)
  (to-json (json/push-string buffer tag) value))

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
         (json/push-string (str value)))
     :lpy
     (let [js-min-int -9007199254740991
           js-max-int  900719925474099]
       (if (<= js-min-int value js-max-int)
         (json/push-long buffer value)
         (-> buffer
             (json/push-string "long")
             (json/push-string (str value)))))))

#?(:joyride (def Long js/Number))
#?(:org.babashka/nbb (def Long js/Number))

(extend-type #?(:cljr System.Int64
                :clj  Long
                :cljs Long
                :lpy  python/int)
  ToJson
  (to-json* [value buffer] (box-long buffer value)))

(defn- ->long [buffer]
  #?(:clj  (Long/parseLong (json/next-string buffer))
     :cljr (System.Int64/Parse (json/next-string buffer))
     :cljs (.fromString Long (json/next-string buffer))
     :lpy  (int (json/next-string buffer))))

(defn is-finite? [value]
  #?(:clj  (Double/isFinite ^Double value)
     :cljr (Double/IsFinite ^Double value)
     :cljs (.isFinite js/Number value)
     :lpy  (math/isfinite value)))

(defn nan? [value]
  #?(:clj  (.equals ^Double value ##NaN)
     :cljr (Double/IsNaN value)
     :cljs (.isNaN js/Number value)
     :lpy  (math/isnan value)))

(defn inf? [value]
  #?(:clj  (.equals ^Double value ##Inf)
     :cljr (Double/IsInfinity value)
     :cljs (== ##Inf value)
     :lpy  (and (math/isinf value) (> value 0))))

(defn -inf? [value]
  #?(:clj  (.equals ^Double value ##-Inf)
     :cljr (Double/IsNegativeInfinity value)
     :cljs (== ##-Inf value)
     :lpy  (and (math/isinf value) (< value 0))))

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

#?(:clj (extend-type Byte    ToJson (to-json* [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Short   ToJson (to-json* [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Integer ToJson (to-json* [value buffer] (json/push-long buffer value))))
#?(:clj (extend-type Float   ToJson (to-json* [value buffer] (push-double buffer value))))
#?(:clj (extend-type Double  ToJson (to-json* [value buffer] (push-double buffer value))))

#?(:cljr (extend System.Byte  ToJson {:to-json* (fn [value buffer] (json/push-long buffer value))}))
#?(:cljr (extend System.Int16 ToJson {:to-json* (fn [value buffer] (json/push-long buffer value))}))
#?(:cljr (extend System.Int32 ToJson {:to-json* (fn [value buffer] (json/push-long buffer value))}))

#?(:cljr (extend-type System.Double  ToJson (to-json* [value buffer] (push-double buffer value))))

#?(:cljs (extend-type number ToJson (to-json* [value buffer] (push-double buffer value))))

#?(:lpy (extend-type python/float ToJson (to-json* [value buffer] (push-double buffer value))))

#?(:clj
   (extend-type clojure.lang.Ratio
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (numerator value))
           (json/push-long (denominator value)))))
   :cljr
   (extend-type clojure.lang.Ratio
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (long (numerator value)))
           (json/push-long (long (denominator value))))))
   :joyride nil
   :org.babashka/nbb nil
   :cljs
   (deftype Ratio [numerator denominator]
     ToJson
     (to-json* [_ buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long numerator)
           (json/push-long denominator)))
     IPrintWithWriter
     (-pr-writer [_this writer _opts]
       (-write writer (str numerator))
       (-write writer "/")
       (-write writer (str denominator))))
   :lpy
   (extend-type fractions/Fraction
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (long (numerator value)))
           (json/push-long (long (denominator value)))))))

(defn ->ratio [buffer]
  (let [n (json/next-long buffer)
        d (json/next-long buffer)]
    #?(:joyride (/ n d)
       :org.babashka/nbb (/ n d)
       :cljs (Ratio. n d)
       :default (/ n d))))

(defn- push-string [buffer value]
  (-> buffer
      (json/push-string "s")
      (json/push-string value)))

(extend-type #?(:cljr System.String
                :clj  String
                :cljs string
                :lpy  python/str)
  ToJson
  (to-json* [value buffer] (push-string buffer value)))

#?(:cljr
   (extend System.Boolean
     ToJson
     {:to-json* (fn [value buffer] (json/push-bool buffer value))})
   :clj
   (extend-type Boolean
     ToJson
     (to-json* [value buffer] (json/push-bool buffer value)))
   :cljs
   (extend-type boolean
     ToJson
     (to-json* [value buffer] (json/push-bool buffer value)))
   :lpy
   (extend-type python/bool
     ToJson
     (to-json* [value buffer] (json/push-bool buffer value))))

(extend-type nil ToJson (to-json* [_value buffer] (json/push-null buffer)))

(defn- can-meta? [value]
  #?(:clj  (instance? clojure.lang.IObj value)
     :cljr (instance? clojure.lang.IObj value)
     :joyride
     (try (with-meta value {}) true
          (catch :default _e false))
     :org.babashka/nbb
     (try (with-meta value {}) true
          (catch :default _e false))
     :cljs (implements? IMeta value)
     :lpy (or (coll? value) (symbol? value))))

(defn- ->meta [buffer]
  (let [m (->value buffer) v (->value buffer)]
    (if-not (can-meta? v) v (with-meta v m))))

(defn- push-meta [buffer m]
  (if (map? m)
    (to-json (json/push-string buffer "^") m)
    buffer))

(defn- tagged-meta [buffer value]
  (push-meta buffer (meta value)))

(defn- bb-fix
  "Remove bb type tag for records which cause an infinite loop."
  [tagged]
  #?(:bb (vary-meta tagged dissoc :type) :default tagged))

(defn- push-tagged [buffer value]
  (-> buffer
      (tagged-meta (bb-fix value))
      (json/push-string (:tag value))
      (to-json (:rep value))))

(defrecord Tagged [tag rep]
  ToJson
  (to-json* [this buffer] (push-tagged buffer this)))

(defmulti tagged-str :tag)
(defmethod tagged-str :default
  [{:keys [tag rep]}]
  (str "#" tag " " (pr-str rep)))

#?(:clj
   (defmethod print-method Tagged [v ^java.io.Writer w]
     (.write w ^String (tagged-str v)))
   :joyride nil
   :org.babashka/nbb nil
   :cljs
   (extend-type Tagged
     IPrintWithWriter
     (-pr-writer [this writer _opts]
       (-write writer (tagged-str this)))))

(defn tagged-value [tag rep] {:pre [(string? tag)]}
  #?(:lpy (assert (string? tag) "only string tags allowed"))
  (->Tagged tag rep))

(defn tagged-value? [x] (instance? Tagged x))

(defn base64-encode [byte-array]
  #?(:clj  (.encodeToString (Base64/getEncoder) byte-array)
     :joyride (.toString (.from js/Buffer byte-array) "base64")
     :org.babashka/nbb (.toString (.from js/Buffer byte-array) "base64")
     :cljs (Base64/encodeByteArray byte-array)
     :cljr (Convert/ToBase64String byte-array)))

(defn base64-decode [string]
  #?(:clj  (.decode (Base64/getDecoder) ^String string)
     :joyride (js/Uint8Array. (.from js/Buffer string "base64"))
     :org.babashka/nbb (js/Uint8Array. (.from js/Buffer string "base64"))
     :cljs (Base64/decodeStringToUint8Array string)
     :cljr (Convert/FromBase64String string)))

(extend-type #?(:clj  #_:clj-kondo/ignore (Class/forName "[B")
                :cljr (Type/GetType "System.Byte[]")
                :cljs js/Uint8Array
                :lpy  python/bytearray)
  ToJson
  (to-json* [value buffer]
    (-> buffer
        (json/push-string "bin")
        (json/push-string (base64-encode value)))))

(defn- ->bin [buffer] (base64-decode (json/next-string buffer)))

(defn push-bigint [buffer value]
  (-> buffer
      (json/push-string "N")
      (json/push-string (str value))))

#?(:clj
   (extend-type clojure.lang.BigInt
     ToJson
     (to-json* [value buffer] (push-bigint buffer value)))
   :cljr
   (extend-type clojure.lang.BigInt
     ToJson
     (to-json* [value buffer] (push-bigint buffer value))))

#?(:cljs
   (m/extend-type?
    js/BigInt
    ToJson
    (to-json* [value buffer] (push-bigint buffer value))))

(defn- ->bigint [buffer]
  #?(:clj  (bigint    (json/next-string buffer))
     :cljr (bigint    (json/next-string buffer))
     :cljs (js/BigInt (json/next-string buffer))))

(defn- push-char [buffer value] (tag buffer "C" (int value)))

#?(:clj
   (extend-type Character
     ToJson
     (to-json* [value buffer] (push-char buffer value)))
   :cljr
   (extend System.Char
     ToJson
     {:to-json* (fn [value buffer] (push-char buffer value))})
   :joyride nil
   :org.babashka/nbb nil

   :cljs
   (deftype Character [code]
     ToJson
     (to-json* [_this buffer]
       (push-char buffer code))
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
     :joyride nil
     :org.babashka/nbb nil
     :cljs (Character. (->value buffer))))

#?(:bb (defn inst-ms [inst] (.getTime inst)))

(defn- push-inst [buffer value]
  (-> buffer
      (json/push-string "inst")
      (json/push-long
       #?(:cljr    (inst-ms (.ToUniversalTime value))
          :lpy     (int (* 1000 (.timestamp value)))
          :default (inst-ms value)))))

(extend-type #?(:clj  Date
                :cljr System.DateTime
                :cljs js/Date
                :lpy  datetime/datetime)
  ToJson
  (to-json* [value buffer]
    (push-inst buffer value)))

(defn- ->inst [buffer]
  #?(:clj  (Date. ^long (json/next-long buffer))
     :cljr (.UtcDateTime
            (System.DateTimeOffset/FromUnixTimeMilliseconds
             (json/next-long buffer)))
     :cljs (js/Date. (json/next-long buffer))
     :lpy  (datetime.datetime/utcfromtimestamp (/ (json/next-long buffer) 1000.0))))

#?(:joyride (def UUID (type (random-uuid))))

(defn- push-uuid [buffer value]
  (-> buffer
      (json/push-string "uuid")
      (json/push-string (str value))))

(extend-type #?(:clj  UUID
                :cljr System.Guid
                :cljs UUID
                :lpy  uuid/UUID)
  ToJson
  (to-json* [value buffer]
    (push-uuid buffer value)))

(defn- ->uuid [buffer]
  #?(:clj  (UUID/fromString (json/next-string buffer))
     :cljr (System.Guid/Parse (json/next-string buffer))
     :cljs (uuid (json/next-string buffer))
     :lpy  (uuid/UUID (json/next-string buffer))))

#?(:clj
   (extend-type URL
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "url")
           (json/push-string (str value)))))
   :cljr
   (extend-type System.Uri
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "url")
           (json/push-string (str value))))))

#?(:cljs
   (m/extend-type?
    js/URL
    ToJson
    (to-json* [value buffer]
              (-> buffer
                  (json/push-string "url")
                  (json/push-string (str value))))))

(defn- ->url [buffer]
  #?(:clj  (URL. (json/next-string buffer))
     :cljr (System.Uri. (json/next-string buffer))
     :cljs (js/URL. (json/next-string buffer))))

#?(:joyride (def Keyword (type :kw)))

(defn- push-keyword [buffer value]
  (if-let [ns (namespace value)]
    (-> buffer
        (json/push-string ";")
        (json/push-string ns)
        (json/push-string (name value)))
    (-> buffer
        (json/push-string ":")
        (json/push-string (name value)))))

(extend-type #?(:clj  clojure.lang.Keyword
                :cljr clojure.lang.Keyword
                :cljs Keyword
                :lpy lang.keyword/Keyword)
  ToJson
  (to-json* [value buffer]
    (push-keyword buffer value)))

(defn- ->keyword [buffer]
  (keyword (json/next-string buffer)))

(defn- ->keyword-2 [buffer]
  (keyword (json/next-string buffer) (json/next-string buffer)))

#?(:joyride (def Symbol (type 'sym)))

(defn- push-symbol [buffer value]
  (if-let [ns (namespace value)]
    (-> buffer
        (tagged-meta value)
        (json/push-string "%")
        (json/push-string ns)
        (json/push-string (name value)))
    (-> buffer
        (tagged-meta value)
        (json/push-string "$")
        (json/push-string (name value)))))

(extend-type #?(:clj  clojure.lang.Symbol
                :cljr clojure.lang.Symbol
                :cljs Symbol
                :lpy  lang.symbol/Symbol)
  ToJson
  (to-json* [value buffer]
    (push-symbol buffer value)))

(defn- ->symbol [buffer]
  (symbol (json/next-string buffer)))

(defn- ->symbol-2 [buffer]
  (symbol (json/next-string buffer) (json/next-string buffer)))

(defn tagged-coll
  ([buffer tag value]
   (tagged-coll buffer tag (meta value) value))
  ([buffer tag meta-map  value]
   (reduce
    to-json
    (-> buffer
        (push-meta meta-map)
        (json/push-string tag)
        (json/push-long (count value)))
    value)))

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
     (to-json* [value buffer]
       (tagged-coll buffer "(" value))))

(def coll-types
  #?(:bb
     [clojure.lang.Cons
      clojure.lang.PersistentList$EmptyList
      clojure.lang.LazySeq
      clojure.lang.ArraySeq
      clojure.lang.APersistentMap$KeySeq
      clojure.lang.APersistentMap$ValSeq
      clojure.lang.LongRange
      clojure.lang.Repeat
      clojure.lang.PersistentList
      clojure.lang.PersistentQueue
      clojure.lang.PersistentVector$ChunkedSeq
      clojure.lang.PersistentArrayMap$Seq]
     :clj
     [clojure.lang.Cons
      clojure.lang.PersistentList$EmptyList
      clojure.lang.LazySeq
      clojure.lang.ArraySeq
      clojure.lang.APersistentMap$KeySeq
      clojure.lang.APersistentMap$ValSeq
      clojure.lang.LongRange
      clojure.lang.Repeat
      clojure.lang.PersistentList
      clojure.lang.ChunkedCons
      clojure.lang.PersistentQueue
      clojure.lang.PersistentVector$ChunkedSeq
      clojure.lang.PersistentArrayMap$Seq]
     :cljr
     [clojure.lang.Cons
      clojure.lang.PersistentList+EmptyList
      clojure.lang.LazySeq
      clojure.lang.ArraySeq
      clojure.lang.APersistentMap+KeySeq
      clojure.lang.APersistentMap+ValSeq
      clojure.lang.LongRange
      clojure.lang.Range
      clojure.lang.Repeat
      clojure.lang.PersistentList
      clojure.lang.PersistentQueue
      clojure.lang.PersistentVector+ChunkedSeq
      clojure.lang.PersistentArrayMap+Seq]
     :joyride
     [(type (cons 1 [])) ;; cljs.core/Cons
      (type (list)) ;; cljs.core/EmptyList
      (type (lazy-seq)) ;; cljs.core/LazySeq
      (type (keys {:a 1})) ;; cljs.core/KeySeq
      (type (vals {:a 1})) ;; cljs.core/ValSeq
      (type (repeat 1)) ;; cljs.core/Repeat
      (type (list 1 2 3)) ;; cljs.core/List
      (type (range)) ;; cljs.core/IntegerRange
      (type (range 10)) ;; cljs.core/Range
      (type (seq {:a 1}))
      (type (seq [1]))]
     :org.babashka/nbb
     [(type (cons 1 [])) ;; cljs.core/Cons
      (type (list)) ;; cljs.core/EmptyList
      (type (lazy-seq)) ;; cljs.core/LazySeq
      (type (keys {:a 1})) ;; cljs.core/KeySeq
      (type (vals {:a 1})) ;; cljs.core/ValSeq
      (type (repeat 1)) ;; cljs.core/Repeat
      (type (list 1 2 3)) ;; cljs.core/List
      (type (range)) ;; cljs.core/IntegerRange
      (type (range 10)) ;; cljs.core/Range
      (type (seq {:a 1}))
      (type (seq [1]))]
     :cljs
     [cljs.core/Cons
      cljs.core/EmptyList
      cljs.core/LazySeq
      cljs.core/IndexedSeq
      cljs.core/KeySeq
      cljs.core/ValSeq
      cljs.core/Repeat
      cljs.core/Range
      cljs.core/List
      cljs.core/ChunkedCons
      cljs.core/ChunkedSeq
      cljs.core/RSeq
      cljs.core/PersistentQueue
      cljs.core/PersistentQueueSeq
      cljs.core/PersistentArrayMapSeq
      cljs.core/PersistentTreeMapSeq
      cljs.core/NodeSeq
      cljs.core/ArrayNodeSeq]
     :lpy
     [lang.seq/LazySeq
      lang.list/PersistentList]))

(doseq [coll-type coll-types]
  #?(:clj
     (extend coll-type
       ToJson
       {:to-json* (fn [value buffer] (tagged-coll buffer "(" value))})
     :cljr
     (extend coll-type
       ToJson
       {:to-json* (fn [value buffer] (tagged-coll buffer "(" value))})
     :cljs
     (extend-type coll-type
       ToJson
       (to-json* [value buffer] (tagged-coll buffer "(" value)))
     :lpy
     (extend-type coll-type
       ToJson
       (to-json* [value buffer] (tagged-coll buffer "(" value)))))

#?(:org.babashka/nbb nil
   :cljs
   (m/extend-type?
    ^:cljs.analyzer/no-resolve
    cljs.core/IntegerRange
    ToJson
    (to-json* [value buffer] (tagged-coll buffer "(" value))))

#?(:clj
   (extend-type clojure.lang.Range
     ToJson
     (to-json* [value buffer] (tagged-coll buffer "(" (into [] value)))))

(def vector-types
  #?(:clj
     [clojure.lang.PersistentVector
      clojure.lang.APersistentVector$SubVector
      clojure.lang.MapEntry]
     :joyride
     [(type [])
      (type (subvec [0 1] 1))
      (type (first {:a 1}))]
     :org.babashka/nbb
     [(type [])
      (type (subvec [0 1] 1))
      (type (first {:a 1}))]
     :cljr
     [clojure.lang.PersistentVector
      clojure.lang.APersistentVector+SubVector
      clojure.lang.MapEntry]
     :cljs
     [cljs.core/PersistentVector
      cljs.core/Subvec
      cljs.core/MapEntry]
     :lpy [lang.vector/PersistentVector]))

(doseq [vector-type vector-types]
  #?(:clj
     (extend vector-type
       ToJson
       {:to-json* (fn [value buffer] (tagged-coll buffer "[" value))})
     :cljr
     (extend vector-type
       ToJson
       {:to-json* (fn [value buffer] (tagged-coll buffer "[" value))})
     :cljs
     (extend-protocol ToJson
       vector-type
       (to-json* [value buffer] (tagged-coll buffer "[" value)))
     :lpy
     (extend-protocol ToJson
       vector-type
       (to-json* [value buffer] (tagged-coll buffer "[" value)))))

(defn- ->into [zero buffer]
  (let [n (json/next-long buffer)]
    (loop [i 0 out (transient zero)]
      (if (== i n)
        (persistent! out)
        (recur
         (unchecked-inc i)
         (conj! out (->value buffer)))))))

#?(:joyride (def PersistentHashSet (type #{1})))
#?(:org.babashka/nbb (def PersistentHashSet (type #{1})))

(extend-type #?(:clj  clojure.lang.PersistentHashSet
                :cljr clojure.lang.PersistentHashSet
                :cljs PersistentHashSet
                :lpy  lang.set/PersistentSet)
  ToJson
  (to-json* [value buffer] (tagged-coll buffer "#" value)))

#?(:joyride (def PersistentTreeSet (type (sorted-set))))
#?(:org.babashka/nbb (def PersistentTreeSet (type (sorted-set))))

#?(:clj
   (extend-type clojure.lang.PersistentTreeSet
     ToJson
     (to-json* [value buffer] (tagged-coll buffer "sset" value)))
   :cljr
   (extend-type clojure.lang.PersistentTreeSet
     ToJson
     (to-json* [value buffer] (tagged-coll buffer "sset" value)))
   :cljs
   (extend-type PersistentTreeSet
     ToJson
     (to-json* [value buffer] (tagged-coll buffer "sset" value))))

(defn- ->sset [buffer]
  #?(:lpy
     (->into #{} buffer)
     :default
     (let [n      (json/next-long buffer)
           values (for [_ (range n)] (->value buffer))
           order  (zipmap values (range))]
       (into
        (sorted-set-by
         (fn [a b]
           (compare (get order a) (get order b))))
        values))))

(defn tagged-map
  ([buffer value]
   (tagged-map buffer "{" value))
  ([buffer tag value]
   (tagged-map buffer tag (meta value) value))
  ([buffer tag meta-map  value]
   (reduce-kv
    (fn [buffer k v]
      (-> buffer
          (to-json k)
          (to-json v)))
    (-> buffer
        (push-meta meta-map)
        (json/push-string tag)
        (json/push-long (count value)))
    value)))

#?(:joyride (def PersistentHashMap (type (hash-map))))
#?(:org.babashka/nbb (def PersistentHashMap (type (hash-map))))

(extend-type #?(:clj  clojure.lang.PersistentHashMap
                :cljr clojure.lang.PersistentHashMap
                :cljs PersistentHashMap
                :lpy  lang.map/PersistentMap)
  ToJson
  (to-json* [value buffer] (tagged-map buffer value)))

#?(:joyride (def PersistentTreeMap (type (sorted-map))))
#?(:org.babashka/nbb (def PersistentTreeMap (type (sorted-map))))

#?(:clj
   (extend-type clojure.lang.PersistentTreeMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer "smap" value)))
   :cljr
   (extend-type clojure.lang.PersistentTreeMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer "smap" value)))
   :cljs
   (extend-type PersistentTreeMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer "smap" value))))

#?(:clj
   (extend-type clojure.lang.APersistentMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer value))))

#?(:joyride (def PersistentArrayMap (type {})))
#?(:org.babashka/nbb (def PersistentArrayMap (type {})))

#?(:clj
   (extend-type clojure.lang.PersistentArrayMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer value)))
   :cljr
   (extend-type clojure.lang.PersistentArrayMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer value)))
   :cljs
   (extend-type PersistentArrayMap
     ToJson
     (to-json* [value buffer] (tagged-map buffer value))))

#?(:clj
   (extend-type clojure.lang.IRecord
     ToJson
     (to-json* [value buffer] (tagged-map buffer value)))
   :cljr
   (extend-type clojure.lang.IRecord
     ToJson
     (to-json* [value buffer] (tagged-map buffer value)))
   :cljs
   (extend-type cljs.core/IRecord
     ToJson
     (to-json* [value buffer] (tagged-map buffer value)))
   :lpy
   (extend-type lang.interfaces/IRecord
     ToJson
     (to-json* [value buffer] (tagged-map buffer value))))

(defn- ->map [buffer]
  (let [n (json/next-long buffer)]
    (loop [i 0 m (transient {})]
      (if (== i n)
        (persistent! m)
        (recur
         (unchecked-inc i)
         (assoc! m (->value buffer) (->value buffer)))))))

(defn- ->sorted-map [buffer]
  #?(:lpy
     (->map buffer)
     :default
     (let [n      (json/next-long buffer)
           pairs  (for [_ (range n)]
                    [(->value buffer) (->value buffer)])
           order  (zipmap (map first pairs) (range))]
       (into
        (sorted-map-by
         (fn [a b]
           (compare (get order a) (get order b))))
        pairs))))

#?(:bb (def clojure.lang.TaggedLiteral (type (tagged-literal 'a :a))))

#?(:joyride (def TaggedLiteral (type (tagged-literal 'f :v))))
#?(:org.babashka/nbb (def TaggedLiteral (type (tagged-literal 'f :v))))

(defn- push-tagged-literal [buffer {:keys [tag form]}]
  (-> buffer
      (json/push-string "tag")
      (json/push-string
       (if-let [ns (namespace tag)]
         (str ns "/" (name tag))
         (name tag)))
      (to-json form)))

#?(:clj
   (extend-type clojure.lang.TaggedLiteral
     ToJson
     (to-json* [value buffer]
       (push-tagged-literal buffer value)))
   :cljr
   (extend-type clojure.lang.TaggedLiteral
     ToJson
     (to-json* [value buffer]
       (push-tagged-literal buffer value)))
   :cljs
   (extend-type TaggedLiteral
     ToJson
     (to-json* [value buffer]
       (push-tagged-literal buffer value)))
   :lpy
   (extend-type lang.tagged/TaggedLiteral
     ToJson
     (to-json* [value buffer]
       (push-tagged-literal buffer value))))

(defn- ->tagged-literal [buffer]
  (tagged-literal (symbol (json/next-string buffer)) (->value buffer)))

(defn- ->list [buffer]
  #?(:lpy     (or (seq (->into [] buffer)) '())
     :default (or (list* (->into [] buffer)) '())))

#?(:clj (defn- eq ^Boolean [^String a b] (.equals a b)))

(defn- ->value [buffer]
  (let [op (json/next-value buffer)]
    (if-not (string? op)
      op
      (transform
       (#?@(:bb [case] :cljr [case] :clj [condp eq] :cljs [case] :lpy [condp identical?])
        op
        "s"    (json/next-string buffer)
        ":"    (->keyword buffer)
        "{"    (->map buffer)
        "$"    (->symbol buffer)
        "["    (->into [] buffer)
        "("    (->list buffer)
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