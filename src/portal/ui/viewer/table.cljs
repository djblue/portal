(ns portal.ui.viewer.table
  (:require ["react" :as react]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defonce ^:private hover (react/createContext nil))

(defn- with-hover [& children]
  (let [state (react/useState nil)]
    (into [:r> (.-Provider hover) #js {:value state}] children)))

(defn- use-hover [] (react/useContext hover))

(defn table [& children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:style
       {:display :grid
        :grid-gap 1
        :background (::c/border theme)
        :border [1 :solid (::c/border theme)]
        :border-radius (:border-radius theme)}}]
     children)))

(defn- cell [row column child]
  (let [background (ins/get-background)
        theme (theme/use-theme)
        [hover set-hover!] (use-hover)]
    [s/div
     {:on-mouse-over
      (fn []
        (set-hover! [row column]))
      :style
      {:background background
       :grid-row (str (inc row))
       :grid-column (str (inc column))}}
     [s/div
      {:style
       (when (and (= (first hover) row)
                  (= (second hover) column))
         {:height "100%"
          :width "100%"
          :background (str (::c/border theme) "55")})}
      child]]))

(defn- special [row column child]
  (let [background (ins/get-background)
        theme      (theme/use-theme)
        [hover]    (use-hover)]
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
         (= (first hover) row)
         {:border-right [3 :solid (::c/boolean theme)]}
         (= (second hover) column)
         {:border-bottom [3 :solid (::c/boolean theme)]})
       {:position :sticky
        :background background
        :grid-row (str (inc row))
        :grid-column (str (inc column))})}
     child]))

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
        cols (seq (ins/try-sort (into #{} (mapcat keys (vals values)))))]
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
        cols (seq (ins/try-sort (into #{} (mapcat keys values))))]
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

    (and (map? value) (every? map? (vals value)))
    inspect-map-table

    (and (coll? value) (every? map? value))
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
