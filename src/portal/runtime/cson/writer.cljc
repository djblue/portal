(ns ^:no-doc portal.runtime.cson.writer
  #?(:clj
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json])
     :cljr
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json])
     :joyride
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json]
      [portal.runtime.macros :as m])
     :org.babashka/nbb
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json]
      [portal.runtime.macros :as m])
     :cljs
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json]
      [portal.runtime.macros :as m])
     :lpy
     (:require
      [portal.runtime.cson.base64 :as base64]
      [portal.runtime.cson.core :as core]
      [portal.runtime.cson.buffer :as json]))
  #?(:clj  (:import [java.net URL]
                    [java.util Date UUID])
     :joyride (:import)
     :org.babashka/nbb (:import)
     :cljs (:import [goog.math Long])
     :lpy  (:import [basilisp.lang :as lang]
                    [datetime :as datetime]
                    [fractions :as fractions]
                    [math :as math]
                    [uuid :as uuid])))

(defprotocol ToJson (to-json* [value buffer]))

(defn to-json [buffer value] (to-json* (core/transform value) buffer))

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

(defn- push-double [buffer value]
  (cond
    (core/is-finite? value) #?(:cljr    (if-not (zero? (mod value 1))
                                          (json/push-double buffer value)
                                          (-> buffer
                                              (json/push-string "D")
                                              (json/push-double value)))
                               :default (json/push-double buffer value))
    (core/nan? value)       (json/push-string buffer "nan")
    (core/inf? value)       (json/push-string buffer "inf")
    (core/-inf? value)      (json/push-string buffer "-inf")))

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
   (extend-type core/Ratio
     ToJson
     (to-json* [this buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (.-numerator this))
           (json/push-long (.-denominator this)))))
   :lpy
   (extend-type fractions/Fraction
     ToJson
     (to-json* [value buffer]
       (-> buffer
           (json/push-string "R")
           (json/push-long (long (numerator value)))
           (json/push-long (long (denominator value)))))))

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

(extend-type #?(:clj portal.runtime.cson.core.Tagged
                :cljr portal.runtime.cson.core.Tagged
                :joyride portal.runtime.cson.core.Tagged
                :org.babashka/nbb portal.runtime.cson.core.Tagged
                :default core/Tagged)
  ToJson
  (to-json* [this buffer] (push-tagged buffer this)))

(extend-type #?(:clj  #_:clj-kondo/ignore (Class/forName "[B")
                :cljr (Type/GetType "System.Byte[]")
                :cljs js/Uint8Array
                :lpy  python/bytes)
  ToJson
  (to-json* [value buffer]
    (-> buffer
        (json/push-string "bin")
        (json/push-string (base64/encode value)))))

(defn- push-bigint [buffer value]
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

(defn- push-char [buffer value]
  (tag buffer "C" #?(:cljs (.-code value) :default (int value))))

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
   (extend-type core/Character
     ToJson
     (to-json* [this buffer]
       (push-char buffer this))))

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
     (to-json* [value buffer]
       (tagged-coll buffer "(" (with-meta
                                 (into [] value)
                                 (meta value))))))

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

(defn tagged-map
  ([buffer value]
   (tagged-map buffer "{" value))
  ([buffer tag value]
   (tagged-map buffer tag (meta value) value))
  ([buffer tag meta-map value]
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

(extend-type #?(:clj  Object
                :cljr Object
                :cljs default
                :lpy  python/object)
  ToJson
  (to-json* [value buffer]
    (if-let [handler (:default-handler core/*options*)]
      (handler buffer value)
      (to-json*
       (with-meta
         (core/tagged-value "remote" (pr-str value))
         (meta value))
       buffer))))

(defn write
  ([value] (write value nil))
  ([value options]
   (binding [core/*options* options]
     (json/with-buffer to-json value))))