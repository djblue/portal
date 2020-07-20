(ns portal.viewer.tree
  (:require [portal.inspector :as ins :refer [inspector]]
            [portal.styled :as s]
            [react-visibility-sensor :default VisibilitySensor]
            [reagent.core :as r]))

(defn delimiter [value]
  (cond
    (map? value)    ["{" "}"]
    (set? value)    ["#{" "}"]
    (vector? value) ["[" "]"]
    :else           ["(" ")"]))

(declare inspect-tree)
(declare inspect-tree-1)

(defn inspect-tree-item []
  (let [open? (r/atom true)]
    (fn [settings k value child]
      (if-not (coll? value)
        [s/div
         {:style {:display :flex}}
         (when-not (= k ::root)
           [s/div {:style {:width :auto}}
            [inspect-tree-1 settings k]])
         child]
        (let [[open close] (delimiter value)
              color (first (:portal/rainbow settings))]
          [s/div
           [s/div {:style {:display :flex :align-items :center}}
            [s/div
             {:on-click #(swap! open? not)
              :style
              {:margin-right "8px"
               :padding-top (:spacing/padding settings)
               :padding-bottom (:spacing/padding settings)}}
             (if @open? "▼" "▶")]
            (when-not (= k ::root)
              [s/div {:style {:width :auto}}
               [inspect-tree-1 settings k]])
            [s/div
             {:style
              {:color color
               :font-weigth :bold
               :margin-right "8px"}} open]
            (when-not @open?
              [:<>
               [s/div
                {:style
                 {:margin-right "8px"}} "..."]
               [s/div
                {:style
                 {:color color
                  :font-weigth :bold
                  :margin-right "8px"}} close]])]
           (when @open?
             [:<>
              [s/div
               {:style
                {:border-left (str "1px dashed " color "55")
                 :padding-left "16px"
                 :margin-left "0.3em"
                 :flex-direction :column
                 :flex-wrap :wrap
                 :align-items :top}}
               child]
              [s/div {:style
                      {:color color
                       :font-weigth :bold
                       :padding-top (:spacing/padding settings)
                       :padding-bottom (:spacing/padding settings)}}
               close]])])))))

(defn for-lazy []
  (let [n (r/atom 0)]
    (fn [seqable]
      [:<>
       (take @n seqable)
       (when (seq (drop @n seqable))
         [:> VisibilitySensor
          {:key @n
           :on-change
           #(when % (swap! n + 5))}
          [s/div {:style {:height "1em"}}]])])))

(defn inspect-tree-map [settings value]
  [s/div
   {:style
    {:padding-left "16px"
     :flex-direction :column
     :flex-wrap :wrap
     :align-items :top}}
   [for-lazy
    (for [[k v] value]
      ^{:key (hash k)}
      [inspect-tree-item settings k v [inspect-tree settings v]])]])

(defn inspect-tree-coll [settings value]
  [s/div
   {:style
    {:padding-left "16px"
     :flex-direction :column
     :flex-wrap :wrap
     :align-items :top}}
   [for-lazy
    (map-indexed
     (fn [idx item]
       ^{:key idx}
       [s/div
        [inspect-tree-item settings ::root item [inspect-tree settings item]]])
     value)]])

(defn inspect-tree [settings value]
  (let [settings (update settings :portal/rainbow rest)]
    [s/div
     {:style {:width :auto}}
     (cond
       (map? value)  [inspect-tree-map settings value]
       (coll? value) [inspect-tree-coll settings value]
       :else         [inspector settings value])]))

(defn inspect-tree-1 [settings value]
  [inspect-tree-item settings ::root value [inspect-tree settings value]])
