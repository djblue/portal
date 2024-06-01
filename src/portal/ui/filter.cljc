(ns ^:no-doc portal.ui.filter
  (:require [clojure.string :as str]
            #?(:cljs [portal.ui.rpc.runtime :as rt])))

#?(:clj (defn regexp? [value] (instance? java.util.regex.Pattern value)))

(defn- match* [value pattern]
  (cond
    (or (nil? value)
        (boolean? value)
        (number? value)
        (string? value)
        (keyword? value)
        (symbol? value)
        (regexp? value))
    (re-find pattern (str value))

    #?(:cljs (rt/runtime? value))
    #?(:cljs (re-find pattern (pr-str value)))

    (map? value)
    (some
     (fn [[k v]]
       (or (match* k pattern) (match* v pattern)))
     value)

    (coll? value)
    (some
     (fn [v]
       (match* v pattern))
     value)

    :else false))

(defn- ->pattern [search-text]
  (when-not (str/blank? search-text)
    (let [text    (str/replace search-text #"[.*+?^${}()|\[\]\\]" "\\$&")
          tokens  (str/split text #"\s+")]
      (re-pattern (str "(?i)" (str/join "|" tokens))))))

(defn- ->true [_] true)

(defn match
  ([search-text]
   (if-let [pattern (->pattern search-text)]
     (fn matcher [value]
       (match* value pattern))
     ->true))
  ([value search-text]
   (if-let [pattern (->pattern search-text)]
     (match* value pattern)
     true)))

(defn filter-value [value text]
  (if-let [pattern (->pattern text)]
    (let [matcher #(match* % pattern)]
      (cond
        (map? value)
        (persistent!
         (reduce-kv
          (fn [result k v]
            (if-not (or (matcher k)
                        (matcher v))
              result
              (assoc! result k v)))
          (transient {})
          value))

        (or (seq? value) (list? value))
        (filter matcher value)

        (coll? value)
        (into (empty value) (filter matcher) value)

        (matcher value) value

        :else ::not-found))
    value))

(defn re-index
  ([re s]
   (re-index re s 0))
  ([re s from-index]
   #?(:clj  (let [m (.matcher ^java.util.regex.Pattern re ^String s)]
              (when (and (.find m ^long from-index)) (.start m)))
      :cljs (some-> (.exec re (subs s from-index)) .-index (+ from-index)))))

(defn split [s search-words]
  (let [string-length (count s)]
    (loop [i            0
           search-words search-words
           out          (transient [])]
      (if (or (= i string-length) (empty? search-words))
        (seq (persistent! out))
        (let [search-words
              (keep
               (fn [{:keys [substring] :as search-word}]
                 (when-let [start (re-index (->pattern substring) s i)]
                   (let [end (+ start (count substring))]
                     (assoc search-word :start start :end end))))
               search-words)]
          (if-let [{:keys [start end] :as entry}
                   (first (sort-by :start search-words))]
            (recur
             (long end)
             search-words
             (cond-> out
               (not= start i) (conj! {:start i :end start})
               :always        (conj! entry)))
            (recur
             string-length
             search-words
             (conj! out {:start i :end string-length}))))))))
