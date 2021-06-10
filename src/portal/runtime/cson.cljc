(ns portal.runtime.cson
  "Clojure/Script Object Notation"
  (:refer-clojure :exclude [read])
  #?(:clj  (:require [cheshire.core :as json])
     :cljs (:require [goog.crypt.base64 :as Base64]
                     [portal.runtime.macros :as m])
     :clje (:require [jiffy]))
  #?(:clj  (:import [java.net URL]
                    [java.util Base64 Date UUID])))

#?(:clje (def double? float?))

(defn- primitive? [value]
  (or (int? value)
      (float? value)
      (double? value)
      (string? value)
      (boolean? value)))

(defprotocol ToJson (-to-json [value]))

(defn tag
  ([tag]     #?(:clj [tag]
                :cljs #js [tag]
                :clje #erl (tag)))
  ([tag a]   #?(:clj [tag a]
                :cljs #js [tag a]
                :clje #erl (tag a)))
  ([tag a b] #?(:clj [tag a b]
                :cljs #js [tag a b]
                :clje #erl (tag a b))))

(defn- can-meta? [value]
  #?(:clj  (instance? clojure.lang.IObj value)
     :cljs (implements? IMeta value)
     :clje (and (satisfies? clojerl.IMeta value)
                (instance? clojerl.Map (meta value)))))

(declare json->)

(def ^:dynamic *transform* nil)

(defn to-json [value]
  (let [value (*transform* value)]
    (cond
      (nil? value) (tag "nil")

      (primitive? value) value

      :else
      (if-let [m (when (can-meta? value) (meta value))]
        (tag
         "meta"
         (-to-json value)
         (-to-json m))
        (-to-json value)))))

(defn- meta-> [value]
  (let [[_ obj m] value]
    (with-meta
      (json-> obj)
      (json-> m))))

(defn- base64-encode [byte-array]
  #?(:clj  (.encodeToString (Base64/getEncoder) byte-array)
     :cljs (Base64/encodeByteArray byte-array)))

(defn- base64-decode [string]
  #?(:clj  (.decode (Base64/getDecoder) ^String string)
     :cljs (Base64/decodeStringToUint8Array string)))

(extend-type #?(:clj  (Class/forName "[B")
                :cljs js/Uint8Array
                :clje nil)
  ToJson
  (-to-json [value]
    (tag "bin" (base64-encode value))))

(defn- bin-> [value] (base64-decode (second value)))

(extend-type #?(:clj  clojure.lang.BigInt
                :cljs js/BigInt
                :clje nil)
  ToJson
  (-to-json [value] (tag "bigint" (str value))))

(defn- bigint-> [value]
  #?(:clj  (bigint    (second value))
     :cljs (js/BigInt (second value))))

#?(:clj
   (extend-type Character
     ToJson
     (-to-json [value] (tag "char" (int value)))))

(defn- char-> [value] (char (second value)))

#?(:bb (defn inst-ms [inst] (.getTime inst)))

(extend-type #?(:clj  Date
                :cljs js/Date
                :clje erlang.util.Date)
  ToJson
  (-to-json [value] (tag "inst" (inst-ms value))))

(defn- inst-> [value]
  #?(:clj  (Date. ^long (second value))
     :cljs (js/Date. (second value))
     :clje (erlang.util.Date. (quot (second value) 1000))))

(extend-type #?(:clj  UUID
                :cljs cljs.core/UUID
                :clje erlang.util.UUID)
  ToJson
  (-to-json [value] (tag "uuid" (str value))))

(defn- uuid-> [value]
  #?(:clj  (UUID/fromString (second value))
     :cljs (uuid (second value))
     :clje (erlang.util.UUID. (second value))))

(extend-type #?(:clj  URL
                :cljs js/URL
                :clje nil)
  ToJson
  (-to-json [value] (tag "url" (str value))))

(defn- url-> [value]
  #?(:clj  (URL. (second value))
     :cljs (js/URL. (second value))))

(extend-type #?(:clj  clojure.lang.Keyword
                :cljs cljs.core/Keyword
                :clje clojerl.Keyword)
  ToJson
  (-to-json [value]
    (if-let [ns (namespace value)]
      (tag "kw" ns (name value))
      (tag "kw" (name value)))))

(defn- kw-> [value]
  (let [[_ ns name] value]
    (if-not name (keyword ns) (keyword ns name))))

(extend-type #?(:clj  clojure.lang.Symbol
                :cljs cljs.core/Symbol
                :clje clojerl.Symbol)
  ToJson
  (-to-json [value]
    (if-let [ns (namespace value)]
      (tag "sym" ns (name value))
      (tag "sym" (name value)))))

(defn- sym-> [value]
  (let [[_ ns name] value]
    (if-not name (symbol ns) (symbol ns name))))

(defn- tagged-list [tag value]
  #?(:clj  (into [tag] (map to-json) value)
     :cljs (.concat #js [tag] (.from js/Array value to-json))
     :clje (seq (into [tag] (map to-json) value))))

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
                :cljs cljs.core/Cons
                :clje clojerl.Cons)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.PersistentList$EmptyList
                :cljs cljs.core/EmptyList
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.LazySeq
                :cljs cljs.core/LazySeq
                :clje clojerl.LazySeq)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.ArraySeq
                :cljs cljs.core/IndexedSeq
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$KeySeq
                :cljs cljs.core/KeySeq
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.APersistentMap$ValSeq
                :cljs cljs.core/ValSeq
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

#?(:clj
   (extend-type clojure.lang.LongRange
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:cljs
   (m/extend-type?
    cljs.core/IntegerRange
    ToJson
    (-to-json [value] (tagged-list "list" value))))

(extend-type #?(:clj  clojure.lang.Range
                :cljs cljs.core/Range
                :clje clojerl.Range)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.Repeat
                :cljs cljs.core/Repeat
                :clje clojerl.Repeat)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.PersistentList
                :cljs cljs.core/List
                :clje clojerl.List)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

(extend-type #?(:clj  clojure.lang.PersistentQueue
                :cljs cljs.core/PersistentQueue
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

#?(:cljs
   (extend-type cljs.core/RSeq
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:cljs
   (extend-type cljs.core/ChunkedCons
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:clje
   (extend-type clojerl.ChunkedCons
     ToJson
     (-to-json [value] (tagged-list "list" value))))

(extend-type #?(:clj  clojure.lang.PersistentVector$ChunkedSeq
                :cljs cljs.core/ChunkedSeq
                :clje clojerl.Vector.ChunkedSeq)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

#?(:cljs
   (extend-type cljs.core/PersistentQueueSeq
     ToJson
     (-to-json [value] (tagged-list "list" value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap$Seq
                :cljs cljs.core/PersistentArrayMapSeq
                :clje clojerl.Vector.Seq)
  ToJson
  (-to-json [value] (tagged-list "list" value)))

#?(:cljs
   (extend-type cljs.core/PersistentTreeMapSeq
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:cljs
   (extend-type cljs.core/NodeSeq
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:cljs
   (extend-type cljs.core/ArrayNodeSeq
     ToJson
     (-to-json [value] (tagged-list "list" value))))

#?(:clje
   (extend-type erlang.List
     ToJson
     (-to-json [value] (tagged-list "list" value))))

(defn- list-> [value]
  (let [result (map json-> (rest value))]
    (dorun result)
    result))

(extend-type #?(:clj  clojure.lang.PersistentVector
                :cljs cljs.core/PersistentVector
                :clje clojerl.Vector)
  ToJson
  (-to-json [value] (tagged-list "vec" value)))

(extend-type #?(:clj  clojure.lang.APersistentVector$SubVector
                :cljs cljs.core/Subvec
                :clje clojerl.Subvec)
  ToJson
  (-to-json [value] (tagged-list "vec" value)))

(extend-type #?(:clj clojure.lang.MapEntry
                :cljs cljs.core/MapEntry
                :clje nil)
  ToJson
  (-to-json [value] (tagged-list "vec" (into [] value))))

(defn- vec-> [value] (into [] (map json->) (rest value)))

(extend-type #?(:clj  clojure.lang.PersistentHashSet
                :cljs cljs.core/PersistentHashSet
                :clje clojerl.Set)
  ToJson
  (-to-json [value] (tagged-list "set" value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeSet
                :cljs cljs.core/PersistentTreeSet
                :clje clojerl.SortedSet)
  ToJson
  (-to-json [value] (tagged-list "set" value)))

(defn- set-> [value] (into #{} (map json->) (rest value)))

(defn- tagged-map [value]
  (tagged-list "map" (mapcat identity value)))

(extend-type #?(:clj  clojure.lang.PersistentHashMap
                :cljs cljs.core/PersistentHashMap
                :clje clojerl.Map)
  ToJson
  (-to-json [value] (tagged-map value)))

(extend-type #?(:clj  clojure.lang.PersistentTreeMap
                :cljs cljs.core/PersistentTreeMap
                :clje clojerl.SortedMap)
  ToJson
  (-to-json [value] (tagged-map value)))

#?(:clj
   (extend-type clojure.lang.APersistentMap
     ToJson
     (-to-json [value] (tagged-map value))))

(extend-type #?(:clj  clojure.lang.PersistentArrayMap
                :cljs cljs.core/PersistentArrayMap
                :clje clojerl.TupleMap)
  ToJson
  (-to-json [value] (tagged-map value)))

(defn- map-> [value]
  (apply hash-map (map json-> (rest value))))

(def ^:dynamic *default-handler* nil)

(defn- dispatch-value [value]
  (case (first value)
    "nil"     nil
    "bigint"  (bigint-> value)
    "bin"     (bin-> value)
    "char"    (char-> value)
    "inst"    (inst-> value)
    "kw"      (kw-> value)
    "list"    (list-> value)
    "map"     (map-> value)
    "meta"    (meta-> value)
    "set"     (set-> value)
    "sym"     (sym-> value)
    "url"     (url-> value)
    "uuid"    (uuid-> value)
    "vec"     (vec-> value)
    (*default-handler* value)))

(defn json-> [value]
  (if #?(:clj  (not (primitive? value))
         :cljs ^boolean (.isArray js/Array value)
         :clje (not (primitive? value)))
    (dispatch-value value)
    value))

(defn write
  ([value] (write value identity))
  ([value transform]
   (binding [*transform* transform]
     #?(:clj  (json/generate-string (to-json value))
        :cljs (.stringify js/JSON (to-json value))
        :clje (jiffy/encode (to-json value))))))

(defn read
  ([string] (read string identity))
  ([string default-handler]
   (binding [*default-handler* default-handler]
     (json->
      #?(:clj  (json/parse-string string)
         :cljs (.parse js/JSON string)
         :clje (jiffy/decode string))))))
