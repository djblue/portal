(ns ^:no-doc portal.ui.filter
  (:require [clojure.string :as str]
            [portal.ui.rpc.runtime :as rt]))

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

    (rt/runtime? value)
    (re-find pattern (pr-str value))

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

(defn match [value search-text]
  (if-let [pattern (->pattern search-text)]
    (match* value pattern)
    true))

(defn- add-filter-meta [value old-value]
  (with-meta
    value
    (-> (meta old-value)
        (assoc ::value old-value)
        (dissoc :portal.runtime/id))))

(defn filter-value [value text]
  (if-let [pattern (->pattern text)]
    (let [matcher #(match* % pattern)]
      (cond
        (map? value)
        (add-filter-meta
         (persistent!
          (reduce-kv
           (fn [result k v]
             (if-not (or (matcher k)
                         (matcher v))
               result
               (assoc! result k v)))
           (transient {})
           value))
         value)

        (or (seq? value) (list? value))
        (add-filter-meta (filter matcher value) value)

        (coll? value)
        (add-filter-meta
         (into (empty value) (filter matcher) value)
         value)

        (matcher value) value

        :else ::not-found))
    value))

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
                 (when-let [start (str/index-of s substring i)]
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
