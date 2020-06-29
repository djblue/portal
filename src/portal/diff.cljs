(ns portal.diff
  (:require [clojure.data :as d]
            [portal.styled :as s]
            [portal.colors :as c]
            [portal.inspector :as ins :refer [inspector]]))

(declare inspect-diff)

(defn can-view? [value]
  (and (vector? value) (= 2 (count value))))

(defn diff-added [settings value]
  (let [color (::c/diff-add settings)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border (str "1px solid " color)
              :border-radius (:border-radius settings)}}
     [inspector settings value]]))

(defn diff-removed [settings value]
  (let [color (::c/diff-remove settings)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border (str "1px solid " color)
              :border-radius (:border-radius settings)}}
     [inspector settings value]]))

(defn diff-map [settings a b]
  (let [ks (into (set (keys a)) (keys b))]
    [ins/container-map
     settings
     (for [k ks
           :let [a (get a k ::not-found)
                 b (get b k ::not-found)]]
       [:<>
        {:key (hash k)}
        [ins/container-map-k
         settings
         (cond
           (= a ::not-found) [diff-added settings k]
           (= b ::not-found) [diff-removed settings k]
           :else             [inspector settings k])]
        [ins/container-map-v
         settings
         (cond
           (= a b)           [inspector settings a]
           (= a ::not-found) [diff-added settings b]
           (= b ::not-found) [diff-removed settings a]
           :else             [inspect-diff settings [a b]])]])]))

(defn diff-set [settings a b]
  (let [[removed added both] (d/diff a b)
        settings (update settings :depth inc)]
    [ins/container-coll
     settings
     [:<>
      [diff-removed settings removed]
      [inspector settings both]
      [diff-added settings added]]]))

(defn diff-vector [settings a b]
  (let [m (max (count a) (count b))
        settings (update settings :depth inc)]
    [ins/container-coll
     settings
     (for [i (range m)
           :let [a (get a i ::not-found)
                 b (get b i ::not-found)]]
       [:<>
        {:key i}
        (cond
          (= a b)           [inspector settings a]
          (= a ::not-found) [diff-added settings b]
          (= b ::not-found) [diff-removed settings a]
          :else             [inspect-diff settings [a b]])])]))

(defn diff-default [settings a b]
  (if (= a b)
    [inspector settings a]
    [s/div
     {:style {:display :flex :width "100%"}}
     [s/div {:style
             {:flex 1
              :margin-right (:spacing/padding settings)}}
      [diff-removed settings a]]
     [s/div {:style {:flex 1}}
      [diff-added settings b]]]))

(defn merge-inspector [settings t a b]
  (case t
    :map    [diff-map settings a b]
    :vector [diff-vector settings a b]
    :set    [diff-set settings a b]
    [diff-default settings a b]))

(defn inspect-diff [settings value]
  (let [[a b] value
        a-type (ins/get-value-type a)
        b-type (ins/get-value-type b)]
    (cond
      (= a b)           [inspector settings a]
      (= a-type b-type) [merge-inspector settings a-type a b]
      :else             [diff-default settings a b])))

