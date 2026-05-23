(ns ^:no-doc portal.runtime.cson.core
  (:refer-clojure :exclude [char])
  #?(:lpy (:import [math :as math])))

(defonce ^:dynamic *options* nil)

(defn transform [value]
  (if-let [f (:transform *options*)]
    (f value)
    value))

(defrecord Tagged [tag rep])

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

#?(:joyride nil
   :org.babashka/nbb nil

   :cljs
   (deftype Character [code]
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

(defn char [code]
  #?(:joyride (clojure.core/char code)
     :org.babashka/nbb (clojure.core/char code)
     :cljs (Character. code)
     :lpy (basilisp.core/char code)
     :default (clojure.core/char code)))

#?(:joyride nil
   :org.babashka/nbb nil

   :cljs
   (deftype Ratio [numerator denominator]
     IPrintWithWriter
     (-pr-writer [_this writer _opts]
       (-write writer (str numerator))
       (-write writer "/")
       (-write writer (str denominator)))))

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