(ns ^:no-doc portal.runtime.cson.reader
  (:refer-clojure :exclude [read])
  (:require
   [portal.runtime.cson.base64 :as base64]
   [portal.runtime.cson.buffer :as json]
   [portal.runtime.cson.core :as core])
  #?(:clj  (:import [java.net URL]
                    [java.util Date UUID])
     :joyride (:import)
     :org.babashka/nbb (:import)
     :cljs (:import [goog.math Long])
     :lpy  (:import [basilisp.lang :as lang]
                    [datetime :as datetime]
                    [uuid :as uuid])))

(declare ->value)

#?(:joyride (def Long js/Number))
#?(:org.babashka/nbb (def Long js/Number))

(defn- ->long [buffer]
  #?(:clj  (Long/parseLong (json/next-string buffer))
     :cljr (System.Int64/Parse (json/next-string buffer))
     :cljs (.fromString Long (json/next-string buffer))
     :lpy  (int (json/next-string buffer))))

(defn ->double [buffer] (json/next-double buffer))

(defn ->ratio [buffer]
  (let [n (json/next-long buffer)
        d (json/next-long buffer)]
    #?(:joyride (/ n d)
       :org.babashka/nbb (/ n d)
       :cljs (core/Ratio. n d)
       :default (/ n d))))

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

(defn- ->bin [buffer] (base64/decode (json/next-string buffer)))

(defn- ->bigint [buffer]
  #?(:clj  (bigint    (json/next-string buffer))
     :cljr (bigint    (json/next-string buffer))
     :cljs (js/BigInt (json/next-string buffer))))

(defn- ->char [buffer]
  (core/char (->value buffer)))

(defn- ->inst [buffer]
  #?(:clj  (Date. ^long (json/next-long buffer))
     :cljr (.UtcDateTime
            (System.DateTimeOffset/FromUnixTimeMilliseconds
             (json/next-long buffer)))
     :cljs (js/Date. (json/next-long buffer))
     :lpy  (datetime.datetime/utcfromtimestamp (/ (json/next-long buffer) 1000.0))))

(defn- ->uuid [buffer]
  #?(:clj  (UUID/fromString (json/next-string buffer))
     :cljr (System.Guid/Parse (json/next-string buffer))
     :cljs (uuid (json/next-string buffer))
     :lpy  (uuid/UUID (json/next-string buffer))))

(defn- ->url [buffer]
  #?(:clj  (URL. (json/next-string buffer))
     :cljr (System.Uri. (json/next-string buffer))
     :cljs (js/URL. (json/next-string buffer))))

(defn- ->keyword [buffer]
  (keyword (json/next-string buffer)))

(defn- ->keyword-2 [buffer]
  (keyword (json/next-string buffer) (json/next-string buffer)))

(defn- ->symbol [buffer]
  (symbol (json/next-string buffer)))

(defn- ->symbol-2 [buffer]
  (symbol (json/next-string buffer) (json/next-string buffer)))

(defn- ->into [zero buffer]
  (let [n (json/next-long buffer)]
    (loop [i 0 out (transient zero)]
      (if (== i n)
        (persistent! out)
        (recur
         (unchecked-inc i)
         (conj! out (->value buffer)))))))

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
      (core/transform
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
        (let [handler (:default-handler core/*options* core/tagged-value)]
          (handler op (->value buffer))))))))

(defn read
  ([string] (read string nil))
  ([string options]
   (binding [core/*options* options]
     (->value (json/->reader string)))))