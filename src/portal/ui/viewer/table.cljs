(ns portal.ui.viewer.table
  (:require ["react" :as react]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(defonce ^:private hover (react/createContext nil))

(defn- with-hover [& children]
  (r/with-let [value (r/atom nil)]
    (into [:r> (.-Provider hover) #js {:value value}] children)))

(defn- use-hover [] (react/useContext hover))

(defn- hover? [hover selector value] (= (selector @hover) value))

(defn- table [& children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:style
       {:display :grid
        :grid-gap 1
        :background (::c/border theme)
        :border [1 :solid (::c/border theme)]
        :border-radius (:border-radius theme)
        :grid-template-columns :min-content}}]
     children)))

(defn- cell [row column child]
  (let [background (ins/get-background)
        theme      (theme/use-theme)
        hover      (use-hover)]
    [s/div
     {:on-mouse-over
      (fn []
        (reset! hover [row column]))
      :style
      {:background background
       :grid-row (str (inc row))
       :grid-column (str (inc column))}}
     [s/div
      {:style {:height "100%" :width "100%"}
       :style/hover
       {:background (str (::c/border theme) "55")}}
      [select/with-position {:row row :column column} child]]]))

(defn- special [row column child]
  (let [background (ins/get-background)
        theme      (theme/use-theme)
        hover      (use-hover)]
    [s/div
     {:style
      (merge
       (cond
         (= 0 row column)
         {:z-index 3
          :top 0
          :left 0
          :border-bottom [3 :solid (::c/border theme)]
          :border-right [3 :solid (::c/border theme)]}
         (zero? row)
         {:z-index 2
          :top 0
          :text-align :center
          :border-bottom [3 :solid (::c/border theme)]}
         (zero? column)
         {:z-index 1
          :left 0
          :text-align :right
          :border-right [3 :solid (::c/border theme)]})
       (cond
         @(r/track hover? hover first row)
         {:border-right [3 :solid (::c/boolean theme)]}
         @(r/track hover? hover second column)
         {:border-bottom [3 :solid (::c/boolean theme)]})
       {:position :sticky
        :background background
        :box-sizing :border-box
        :padding (:padding theme)
        :grid-row (str (inc row))
        :grid-column (str (inc column))})}
     [select/with-position {:row row :column column} child]]))

(defn- columns [cols]
  [:<>
   [special 0 0]
   (map-indexed
    (fn [col-index column]
      ^{:key (hash column)}
      [ins/with-key column
       [special 0 (inc col-index) [ins/inspector column]]])
    cols)])

(defn- inspect-map-table [values]
  (let [rows (seq (ins/try-sort (keys values)))
        cols (or (:portal.viewer.table/columns (meta values))
                 (seq (ins/try-sort (into #{} (mapcat keys (vals values))))))]
    [table
     [columns cols]
     [l/lazy-seq
      (map-indexed
       (fn [row-index row]
         [:<>
          {:key (hash row)}
          [ins/with-key row
           [special (inc row-index) 0 [ins/inspector row]]]
          [ins/inc-depth
           [ins/with-key row
            (map-indexed
             (fn [col-index column]
               (let [coll (get values row)]
                 ^{:key col-index}
                 [ins/with-collection coll
                  [ins/with-key column
                   [cell
                    (inc row-index)
                    (inc col-index)
                    (when (contains? coll column) [ins/inspector (get coll column)])]]]))
             cols)]]])
       rows)]]))

(defn- inspect-coll-table [values]
  (let [rows (seq values)
        cols (or (:portal.viewer.table/columns (meta values))
                 (seq (ins/try-sort (into #{} (mapcat keys values)))))]
    [table
     [columns cols]
     [l/lazy-seq
      (map-indexed
       (fn [row-index row]
         ^{:key row-index}
         [:<>
          [ins/with-key row-index
           [special (inc row-index) 0 [ins/inspector row-index]]]
          [ins/inc-depth
           [ins/with-key row-index
            (map-indexed
             (fn [col-index column]
               ^{:key col-index}
               [ins/with-collection row
                [ins/with-key column
                 [cell
                  (inc row-index)
                  (inc col-index)
                  (when (contains? row column) [ins/inspector (get row column)])]]])
             cols)]]])
       rows)]]))

(defn- inspect-vector-table [values]
  [table
   [l/lazy-seq
    (map-indexed
     (fn [row-index row]
       ^{:key row-index}
       [:<>
        [ins/with-key row-index
         [special (inc row-index) 0 [ins/inspector row-index]]]
        [ins/inc-depth
         [ins/with-key row-index
          (map-indexed
           (fn [col-index value]
             ^{:key col-index}
             [ins/with-collection row
              [ins/with-key col-index
               [cell
                (inc row-index)
                (inc col-index)
                [ins/inspector value]]]])
           row)]]])
     values)]])

(defn- get-component [value]
  (cond
    (and (or (vector? value) (list? value))
         (every? vector? value))
    inspect-vector-table

    (and (ins/map? value) (every? ins/map? (vals value)))
    inspect-map-table

    (and (ins/coll? value) (every? ins/map? value))
    inspect-coll-table))

(defn table-view? [value] (some? (get-component value)))

(defn inspect-table [values]
  (let [component (get-component values)]
    [ins/with-collection values
     [with-hover
      [component values]]]))

(def viewer
  {:predicate table-view?
   :component inspect-table
   :name :portal.viewer/table})
