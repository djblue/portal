(ns portal.ui.viewer.tree
  (:require ["react" :as react]
            [portal.ui.inspector :as ins :refer [inspector]]
            [portal.ui.styled :as s]
            [portal.ui.lazy :as l]
            [portal.ui.theme :as theme]))

(defn- delimiter [value]
  (cond
    (map? value)    ["{" "}"]
    (set? value)    ["#{" "}"]
    (vector? value) ["[" "]"]
    :else           ["(" ")"]))

(declare inspect-tree)
(declare inspect-tree-1)

(def cursor-pointer {:cursor :pointer})
(def select-none	{:user-select :none})
(def flex-wrap      {:flex-wrap :wrap})
(def flex-row       {:display :flex :flex-direction :row})
(def flex-col	    {:display :flex :flex-direction :column})
(def flex-center    {:display :flex :align-items :center})

(defn- use-node-styles []
  (let [theme (theme/use-theme)
        color (theme/use-rainbow)]
    {:color color
     :font-weigth :bold
     :margin-right "8px"
     :padding-top (:spacing/padding theme)
     :padding-bottom (:spacing/padding theme)}))

(defn- center [& children]
  (into [s/div {:style flex-center}] children))

(defn- inspect-tree-item [opts]
  (let [[open? set-open] (react/useState true)
        theme (theme/use-theme)
        value (:value opts)
        [open close] (delimiter value)
        style  (use-node-styles)
        open   [s/div {:style style} open]
        close  [s/div {:style style} close]
        toggle [s/div
                {:on-click #(set-open not)
                 :style (merge style cursor-pointer select-none)}
                (if open? "▼" "▶")]
        child [s/div
               {:style
                (merge
                 {:border-left [1 :dashed (str (:color style) "55")]
                  :padding-right (* 2 (:spacing/padding theme))
                  :margin-left "0.3em"}
                 flex-wrap
                 flex-col)}
               (:value-child opts)]
        ellipsis  [s/div {:style {:margin-right (:spacing/padding theme)}} "..."]]
    (cond
      (not (coll? value))
      [center
       (:key-child opts)
       (:value-child opts)]

      (not (coll? (:key opts)))
      [s/div
       [center
        toggle
        (:key-child opts)
        open
        (when-not open?
          [:<> ellipsis close])]
       (when open?
         [:<> child close])]

      :else
      [s/div
       [center
        (:key-child opts)
        (if-not open?
          [center toggle open ellipsis close]
          [s/div
           {:style flex-col}
           [s/div {:style flex-row} toggle open]
           child
           close])]])))

(defn- inspect-tree-map [settings value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:padding-left (* 2 (:spacing/padding theme))}}
     [l/lazy-seq
      settings
      (for [[k v] (ins/try-sort-map value)]
        ^{:key (hash k)}
        [inspect-tree-item
         {:key k
          :key-child [inspect-tree-1 settings k]
          :value v
          :value-child [inspect-tree settings v]}])]]))

(defn- inspect-tree-coll [settings value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:padding-left (* 2 (:spacing/padding theme))}}
     [l/lazy-seq
      settings
      (map-indexed
       (fn [idx item]
         ^{:key idx}
         [s/div
          [inspect-tree-item
           {:value item
            :value-child [inspect-tree settings item]}]])
       value)]]))

(defn- inspect-tree [settings value]
  (let [settings (update settings :portal/rainbow rest)]
    [theme/cycle-rainbow
     [s/div
      {:style {:width :auto}}
      (cond
        (map? value)  [inspect-tree-map settings value]
        (coll? value) [inspect-tree-coll settings value]
        :else         [inspector settings value])]]))

(defn inspect-tree-1 [settings value]
  [inspect-tree-item
   {:value value
    :value-child [inspect-tree settings value]}])

(defn tree [settings value]
  [ins/inc-depth [inspect-tree-1 settings value]])

(def viewer
  {:predicate coll?
   :component tree
   :name :portal.viewer/tree})
