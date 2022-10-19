(ns portal.ui.viewer.tree
  (:require ["react" :as react]
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

(def ^:private cursor-pointer {:cursor :pointer})
(def ^:private select-none    {:user-select :none})
(def ^:private flex-wrap      {:flex-wrap :wrap})
(def ^:private flex-row       {:display :flex :flex-direction :row})
(def ^:private flex-col       {:display :flex :flex-direction :column})
(def ^:private flex-center    {:display :flex :align-items :center})

(defn- use-node-styles []
  (let [theme (theme/use-theme)
        color (theme/use-rainbow)]
    {:color          color
     :font-weigth    :bold
     :margin-right   (:padding theme)
     :padding-top    (* 0.5 (:padding theme))
     :padding-bottom (* 0.5 (:padding theme))}))

(defn- center [& children]
  (into [s/div {:style flex-center}] children))

(defn- inspect-tree-item [{:keys [values] :as opts}]
  (let [[open? set-open] (react/useState (if-some [expand (get-in (meta values) [:portal.viewer/tree :expand])]
                                           expand
                                           true))
        theme (theme/use-theme)
        value (:value opts)
        [open close] (delimiter value)
        style  (use-node-styles)
        open   [s/div {:style style} open]
        close  [s/div {:style style} close]
        toggle [s/div
                {:on-click
                 (fn [e]
                   (.stopPropagation e)
                   (set-open not))
                 :style (merge style cursor-pointer select-none)}
                (if open? "▼" "▶")]
        child [s/div
               {:style
                (merge
                 {:position :relative
                  :padding-right (:padding theme)
                  :padding-left (:padding theme)
                  :margin-left "0.3em"}
                 flex-wrap
                 flex-col)}
               [s/div
                {:style
                 {:position :absolute
                  :top 0
                  :bottom 0
                  :left 0
                  :opacity 0.4
                  :border-left [1 :dashed (:color style)]}}]
               (:value-child opts)]
        ellipsis  [s/div {:style {:margin-right (:padding theme)}} "..."]]
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
       {:style {:padding-left (:padding theme)}}
       [center
        (:key-child opts)
        (if-not open?
          [center toggle open ellipsis close]
          [s/div
           {:style flex-col}
           [s/div {:style flex-row} toggle open]
           child
           close])]])))

(defn- parent-tree? [context]
  (and (not (true? (:key? context)))
       (map? (:collection context))
       (= (:portal.viewer/default context)
          :portal.viewer/tree)))

(defn- with-tree-item [child]
  (let [context (ins/use-context)]
    (if (parent-tree? context)
      child
      [ins/with-default-viewer
       :portal.viewer/tree
       [inspect-tree-item
        {:value (:value context)
         :value-child child}]])))

(defn- inspect-tree-map [value]
  (let [theme (theme/use-theme)]
    [with-tree-item
     [theme/cycle-rainbow
      [ins/with-collection value
       [l/lazy-seq
        (map-indexed
         (fn [index [k v]]
           ^{:key index}
           [inspect-tree-item
            {:key k
             :key-child
             [ins/with-context
              {:key? true}
              [select/with-position
               {:row index :column 0}
               [s/div
                {:style {:white-space :nowrap
                         :padding-right (:padding theme)}}
                [ins/inspector k]]]]
             :value v
             :value-child
             [s/div
              [ins/with-key k
               [select/with-position
                {:row index :column 1}
                [ins/inspector v]]]]}])
         (ins/try-sort-map value))]]]]))

(defn- inspect-tree-coll [value]
  [with-tree-item
   [theme/cycle-rainbow
    [ins/with-collection value
     [l/lazy-seq
      (map-indexed
       (fn [idx item]
         ^{:key idx}
         [ins/with-key idx
          [select/with-position
           {:row idx :column 0}
           [ins/inspector item]]])
       value)]]]])

(defn inspect-tree [value]
  [ins/inc-depth
   (cond
     (map? value)  [inspect-tree-map value]
     (coll? value) [inspect-tree-coll value])])

(def viewer
  {:predicate ins/coll?
   :component inspect-tree
   :name :portal.viewer/tree})
