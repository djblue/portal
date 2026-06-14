(ns ^:no-doc portal.runtime.cson.writer-simple
  (:require
   [portal.runtime.cson.buffer :as json]
   [portal.runtime.cson.core :as core]))

(declare to-json*)

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

(defn- push-string [buffer value]
  (-> buffer
      (json/push-string "s")
      (json/push-string value)))

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

(defn- bigint? [value]
  #?(:clj  (instance? clojure.lang.BigInt value)
     :cljr (instance? clojure.lang.BigInt value)
     :cljs (identical? js/BigInt (some-> value .-constructor))
     :else false))

(defn- push-bigint [buffer value]
  (-> buffer
      (json/push-string "N")
      (json/push-string (str value))))

(defn- push-char [buffer value]
  (tag buffer "C" #?(:cljs (.-code value) :default (int value))))

(defn- is-char? [value]
  #?(:joyride false
     :org.babashka/nbb false
     :cljs    (instance? core/Character value)
     :lpy     false
     :default (char? value)))

#?(:bb (defn inst-ms [inst] (.getTime inst)))

(defn- push-inst [buffer value]
  (-> buffer
      (json/push-string "inst")
      (json/push-long
       #?(:cljr    (inst-ms (.ToUniversalTime value))
          :lpy     (int (* 1000 (.timestamp value)))
          :default (inst-ms value)))))

(defn- push-uuid [buffer value]
  (-> buffer
      (json/push-string "uuid")
      (json/push-string (str value))))

(defn- push-keyword [buffer value]
  (if-let [ns (namespace value)]
    (-> buffer
        (json/push-string ";")
        (json/push-string ns)
        (json/push-string (name value)))
    (-> buffer
        (json/push-string ":")
        (json/push-string (name value)))))

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

(defn- range? [value]
  #?(:clj  (instance? clojure.lang.Range value)
     :cljr (instance? clojure.lang.Range value)
     :joyride false
     :org.babashka/nbb false
     :cljs (instance? Range value)))

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

(defn- push-tagged-literal [buffer {:keys [tag form]}]
  (-> buffer
      (json/push-string "tag")
      (json/push-string
       (if-let [ns (namespace tag)]
         (str ns "/" (name tag))
         (name tag)))
      (to-json form)))

#?(:lpy (defn- sorted? [_] false))

(defn- push-ratio [buffer _value]
  #?(:cljs buffer
     :default
     (-> buffer
         (json/push-string "R")
         (json/push-long (numerator _value))
         (json/push-long (denominator _value)))))

#?(:cljs (defn- ratio? [_] false))

(defn to-json* [value buffer]
  (cond
    (core/tagged-value? value)
    (push-tagged buffer value)

    (nil? value)      (json/push-null buffer)
    (boolean? value)  (json/push-bool buffer value)
    (is-char? value)  (push-char buffer value)
    (string? value)   (push-string buffer value)
    (bigint? value)   (push-bigint buffer value)
    (number? value)   (cond
                        (ratio? value)  (push-ratio buffer value)
                        (float? value)  (push-double buffer value)
                        :else           (box-long buffer value))
    (keyword? value)  (push-keyword buffer value)
    (symbol? value)   (push-symbol buffer value)
    (map? value)      (cond
                        (sorted? value) (tagged-map buffer "smap" value)
                        :else           (tagged-map buffer value))
    (vector? value)   (tagged-coll buffer "[" value)
    (set? value)      (cond
                        (sorted? value) (tagged-coll buffer "sset" value)
                        :else           (tagged-coll buffer "#" value))
    (seqable? value)  (cond
                        (range? value)  (tagged-coll buffer "(" (with-meta
                                                                  (into [] value)
                                                                  (meta value)))
                        :else           (tagged-coll buffer "(" value))
    (uuid? value)     (push-uuid buffer value)
    (inst? value)     (push-inst buffer value)

    (tagged-literal? value)
    (push-tagged-literal buffer value)

    :else
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