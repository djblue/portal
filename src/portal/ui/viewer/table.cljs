(ns portal.ui.viewer.table
  (:require ["react" :as react]
            [clojure.spec.alpha :as sp]
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
  (let [theme   (theme/use-theme)
        context (ins/use-context)]
    (into
     [s/div
      {:style
       (cond->
        {:display :grid
         :grid-gap 1
         :background (::c/border theme)
         :border [1 :solid (::c/border theme)]
         :border-radius (:border-radius theme)
         :grid-template-columns :min-content}
         (not= (:depth context) 1)
         (assoc :overflow :auto))}]
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
      {:background  background
       :grid-row    (str (inc row))
       :grid-column (str (inc column))}}
     [s/div
      {:style {:height     "100%"
               :width      "100%"
               :box-sizing :border-box
               :padding    (:padding theme)}
       :style/hover
       {:background (str (::c/border theme) "55")}}
      [select/with-position {:row row :column column} child]]]))

(defn- special [row column child span]
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
        :grid-row (str (inc row)
                       (when span (str " / span " span)))
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
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
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
          [ins/toggle-bg
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
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
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
          [ins/toggle-bg
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
        [ins/toggle-bg
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

(sp/def ::row (sp/coll-of map?))
(sp/def ::multi-map (sp/map-of any? ::row))

(defn inspect-multi-map-table [values]
  (let [rows (seq (ins/try-sort (keys values)))
        cols (or (get-in (meta values) [:portal.viewer/table :columns])
                 (seq (ins/try-sort (into #{} (comp (mapcat second) (mapcat keys)) values))))]
    [table
     [columns cols]
     [l/lazy-seq
      (map-indexed
       (fn [row-index {:keys [row value index]}]
         [:<>
          {:key row-index}
          (when (zero? index)
            [ins/with-key row
             [special (inc row-index) 0 [ins/inspector row] (count (get values row))]])
          [ins/toggle-bg
           [ins/with-key row
            (map-indexed
             (fn [col-index column]
               ^{:key col-index}
               [ins/with-collection value
                [ins/with-key index
                 [ins/with-key column
                  [cell
                   (inc row-index)
                   (inc col-index)
                   (when (contains? value column)
                     [ins/inspector (get value column)])]]]])
             cols)]]])
       (mapcat
        (fn [row]
          (map-indexed
           (fn [index value]
             {:row row :value value :index index})
           (get values row)))
        rows))]]))

(defn- get-component [value]
  (cond
    (sp/valid? ::multi-map value)
    inspect-multi-map-table

    (and (ins/map? value) (every? ins/map? (vals value)))
    inspect-map-table

    (and (or (vector? value) (coll? value))
         (every? vector? value))
    inspect-vector-table

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
   :name :portal.viewer/table
   :doc "View value as a table. Supports sticky headers and keyboard navigation."})
