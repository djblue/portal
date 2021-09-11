(ns portal.ui.filter
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
