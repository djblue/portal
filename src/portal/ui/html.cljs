(ns ^:no-doc portal.ui.html
  (:require [clojure.string :as str]
            [portal.ui.inspector :as ins]))

(defn- ->style [string]
  (persistent!
   (reduce
    (fn [style rule]
      (let [[k v] (str/split rule #":")]
        (assoc! style (keyword (str/trim k)) (str/trim v))))
    (transient {})
    (str/split string #";"))))

(defn- dom->hiccup [opts ^js el]
  (let [{:keys [text-handler]} opts]
    (case (.-nodeType el)
      3 (cond-> (.-wholeText el)
          text-handler
          text-handler)
      1 (let [attrs (.-attributes el)]
          (into
           [(keyword (str/lower-case (.-tagName el)))
            (persistent!
             (reduce
              (fn [attrs ^js attr]
                (let [k (keyword (.-name attr))]
                  (assoc! attrs k
                          (case k
                            :style (->style (.-value attr))
                            (.-value attr)))))
              (transient {})
              attrs))]
           (map (partial dom->hiccup opts))
           (.-childNodes el))))))

(defn- parse-dom [string]
  (-> (js/DOMParser.)
      (.parseFromString string "text/html")
      (.getElementsByTagName "body")
      (aget 0)
      (.-childNodes)))

(defn parse-html
  ([html]
   (parse-html html nil))
  ([html opts]
   (into [:<>]
         (map (partial dom->hiccup opts))
         (parse-dom html))))

(def ^:private opts
  {:text-handler
   (fn [text] [ins/highlight-words text])})

(defn html+ [html] (parse-html html opts))