(ns ^:no-doc portal.ui.filter
  (:require [clojure.string :as str]))

(defn match [value text]
  (cond
    (or (nil? value)
        (boolean? value)
        (number? value)
        (string? value)
        (keyword? value)
        (symbol? value))
    (str/includes? (str value) text)

    (map? value)
    (some
     (fn [[k v]]
       (or (match k text) (match v text)))
     value)

    (coll? value)
    (some
     (fn [v]
       (match v text))
     value)

    :else (match (pr-str value) text)))

(defn- add-filter-meta [value old-value]
  (with-meta
    value
    (-> (meta old-value)
        (assoc ::value old-value)
        (dissoc :portal.runtime/id))))

(defn filter-value [value text]
  (if (str/blank? text)
    value
    (let [tokens (str/split text #"\s+")
          matcher (fn [value]
                    (every? #(match value %) tokens))]
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

        (list? value)
        (add-filter-meta (filter matcher value) value)

        (coll? value)
        (add-filter-meta
         (into (empty value) (filter matcher) value)
         value)

        (matcher value) value

        :else ::not-found))))

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
