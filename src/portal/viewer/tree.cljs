(ns portal.viewer.tree
  (:require [portal.inspector :as ins :refer [inspector]]
            [portal.styled :as s]
            [portal.lazy :as l]
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
    (fn [settings opts]
      (let [value (:value opts)
            [open close] (delimiter value)
            color (first (:portal/rainbow settings))
            open [s/div
                  {:style
                   {:color color
                    :font-weigth :bold
                    :margin-right "8px"
                    :padding-top (:spacing/padding settings)
                    :padding-bottom (:spacing/padding settings)}} open]
            close [s/div
                   {:style
                    {:color color
                     :font-weigth :bold
                     :margin-right "8px"
                     :padding-top (:spacing/padding settings)
                     :padding-bottom (:spacing/padding settings)}}
                   close]
            toggle [s/div
                    {:on-click #(swap! open? not)
                     :style
                     {:margin-right "8px"
                      :padding-top (:spacing/padding settings)
                      :padding-bottom (:spacing/padding settings)}}
                    (if @open? "▼" "▶")]
            child [s/div
                   {:style
                    {:border-left (str "1px dashed " color "55")
                     :padding-right 16
                     :margin-left "0.3em"
                     :flex-direction :column
                     :flex-wrap :wrap}}
                   (:value-child opts)]
            ellipsis  [s/div
                       {:style
                        {:margin-right "8px"}} "..."]]
        (cond
          (not (coll? value))
          [s/div
           {:style {:display :flex :align-items :center}}
           (:key-child opts)
           (:value-child opts)]

          (not (coll? (:key opts)))
          [s/div
           [s/div {:style {:display :flex :align-items :center}}
            toggle
            (:key-child opts)
            open
            (when-not @open?
              [:<> ellipsis close])]
           (when @open?
             [:<>
              child
              close])]

          :else
          [s/div
           [s/div {:style {:display :flex :align-items :center}}
            (:key-child opts)
            (if-not @open?
              [s/div
               {:style {:display :flex :align-items :center}}
               toggle
               open
               ellipsis
               close]
              [s/div
               {:style {:display :flex :flex-direction :column}}
               [s/div {:style {:display :flex}} toggle open]
               child
               close])]])))))

(defn inspect-tree-map [settings value]
  [s/div
   {:style
    {:padding-left 16
     :flex-direction :column
     :flex-wrap :wrap}}
   [l/lazy-seq
    (for [[k v] value]
      ^{:key (hash k)}
      [inspect-tree-item settings
       {:key k
        :key-child [inspect-tree-1 settings k]
        :value v
        :value-child [inspect-tree settings v]}])]])

(defn inspect-tree-coll [settings value]
  [s/div
   {:style
    {:padding-left 16
     :flex-direction :column
     :flex-wrap :wrap}}
   [l/lazy-seq
    (map-indexed
     (fn [idx item]
       ^{:key idx}
       [s/div
        [inspect-tree-item settings
         {:value item
          :value-child [inspect-tree settings item]}]])

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
  [inspect-tree-item settings
   {:value value
    :value-child [inspect-tree settings value]}])
