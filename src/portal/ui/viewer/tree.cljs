(ns ^:no-doc portal.ui.viewer.tree
  (:require [portal.ui.filter :as f]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn- delimiter [value]
  (cond
    (map? value)    ["{" "}"]
    (set? value)    ["#{" "}"]
    (vector? value) ["[" "]"]
    :else           ["(" ")"]))

(defn- wrapper [context & children]
  (let [opts   (ins/use-options)
        {:keys [expanded? selected]} opts
        value (:value context)
        [open close] (delimiter value)
        theme (theme/use-theme)
        viewer (get-in opts [:viewer :name])
        border-color (if selected
                       (get theme (nth theme/order selected))
                       "rgba(0,0,0,0)")
        border [1 :solid border-color]
        color (get theme (nth theme/order (:depth context)))
        wrapper-options (ins/use-wrapper-options context)
        background (ins/get-background)
        selected-background (when selected background)]
    (if-not (= viewer :portal.viewer/tree)
      [s/div
       {:style {:grid-row "1"
                :grid-column "3 / 4"
                :padding-left (:padding theme)}}
       (into [ins/wrapper context] children)]
      [:<>
       [s/div {:style {:grid-row "1" :grid-column "1"
                       :width (:padding theme)
                       :text-align :center}}
        [ins/toggle-expand]]
       [s/div {:style (merge
                       {:grid-row "1"
                        :grid-column "3"
                        :color color
                        :position :relative
                        :border-left border
                        :border-right border
                        :border-top border
                        :border-top-right-radius (:border-radius theme)
                        :border-top-left-radius (:border-radius theme)
                        :background selected-background}
                       (when-not expanded?
                         {:border-bottom border
                          :border-bottom-right-radius (:border-radius theme)
                          :border-bottom-left-radius (:border-radius theme)}))
               :on-click (:on-click wrapper-options)}
        open
        (when-not expanded? [:<> close [:sub (count value)]])
        (when (and selected expanded?)
          [s/div
           {:style
            {:position :absolute
             :left 0
             :right 0
             :bottom -1
             :background selected-background
             :height 1}}])]
       (when expanded?
         [s/div {:style
                 {:grid-row "2" :grid-column "1/4"
                  :box-sizing :border-box
                  :padding-left (:padding theme)
                  :border-top border
                  :border-right  border
                  :border-left border
                  :background selected-background
                  :border-top-left-radius (:border-radius theme)}}
          (into [s/div {:style
                        {:position :relative
                         :padding-left (:padding theme)}}
                 [s/div
                  {:style
                   {:position :absolute
                    :top 1
                    :left -3
                    :bottom 4
                    :opacity 0.15
                    :border-left [1 :dashed color]}}]]
                children)])
       (when expanded?
         [s/div
          {:style
           {:grid-row "3" :grid-column "1 / 4"
            :color color
            :border-left border
            :border-right border
            :border-bottom border
            :background selected-background
            :border-bottom-left-radius (:border-radius theme)
            :border-bottom-right-radius (:border-radius theme)}
           :on-click (:on-click wrapper-options)}
          [s/div
           {:style
            {:display :inline-block
             :width (:padding theme)
             :text-align :center}}
           close]])])))

(defn- tree-grid [context & children]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:display :grid
              :grid-template-columns "auto auto 1fr"}}
     [s/div
      {:style {:grid-row "1" :grid-column "2"
               :padding [0 (:padding theme)]}}]
     (into [wrapper context] children)]))

(defn- inspect-tree-map [value]
  (let [theme (theme/use-theme)
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [ins/with-collection value
     [l/lazy-seq
      (keep-indexed
       (fn [idx [k v]]
         (when (or (matcher k) (matcher v))
           (if-not (ins/coll? v)
             ^{:key idx}
             [s/div
              {:style {:display :flex
                       :gap (:padding theme)}}
              [s/div
               [ins/with-key k
                [select/with-position
                 {:row idx :column 0}
                 [ins/inspector {:portal.viewer/inspector {:toggle-bg false}} k]]]]
              [ins/with-key k
               [select/with-position
                {:row idx :column 1}
                [ins/inspector {:portal.viewer/inspector {:toggle-bg false}} v]]]]

             ^{:key idx}
             [ins/toggle-bg
              [s/div
               {:style {:display :grid
                        :grid-template-columns "auto auto 1fr"}}
               [s/div
                {:style {:grid-row "1" :grid-column "2"
                         :padding [0 (:padding theme)]}}
                [ins/with-key k
                 [select/with-position
                  {:row idx :column 0}
                  [ins/inspector
                   {:portal.viewer/default :portal.viewer/tree
                    :portal.viewer/inspector {:wrapper wrapper :toggle-bg false}}
                   k]]]]
               [ins/with-key k
                [select/with-position
                 {:row idx :column 1}
                 [ins/inspector
                  {:portal.viewer/default :portal.viewer/tree
                   :portal.viewer/tree {:parent :tree}
                   :portal.viewer/inspector {:wrapper wrapper :toggle-bg false}}
                  v]]]]])))
       (ins/try-sort-map value))]]))

(defn- inspect-tree-coll [value]
  (let [theme (theme/use-theme)
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [ins/with-collection value
     [l/lazy-seq
      (keep-indexed
       (fn [idx v]
         (when (matcher v)
           (if-not (ins/coll? v)
             ^{:key idx}
             [s/div
              [ins/with-key idx
               [select/with-position
                {:row idx :column 0}
                [ins/inspector
                 {:portal.viewer/inspector {:toggle-bg false}}
                 v]]]]
             ^{:key idx}
             [s/div
              {:style {:display :grid
                       :grid-template-columns "auto auto 1fr"}}
              [s/div
               {:style {:grid-row "1" :grid-column "2"
                        :padding [0 (:padding theme)]}}]
              [ins/with-key idx
               [select/with-position
                {:row idx :column 0}
                [ins/inspector
                 {:portal.viewer/default :portal.viewer/tree
                  :portal.viewer/tree {:parent :tree}
                  :portal.viewer/inspector {:wrapper wrapper :toggle-bg false}}
                 v]]]])))
       value)]]))

(defn inspect-tree [value]
  (let [opts (ins/use-options)
        context (ins/use-context)
        parent (get-in opts [:props :portal.viewer/tree :parent])
        child (cond
                (map? value)  [inspect-tree-map value]
                (coll? value) [inspect-tree-coll value])]
    (if (= parent :tree)
      child
      [tree-grid context child])))

(def viewer
  {:predicate ins/coll?
   :component #'inspect-tree
   :name      :portal.viewer/tree
   :doc       "For viewing highly nested values, such as hiccup."})