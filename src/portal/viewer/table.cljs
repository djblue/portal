(ns portal.viewer.table
  (:require [portal.colors :as c]
            [portal.inspector :as ins :refer [inspector]]
            [portal.lazy :as l]
            [portal.styled :as s]))

(defn- get-styles [settings]
  {:white-space :nowrap
   :border-bottom (str "1px solid " (::c/border settings))
   :border-right (str "1px solid " (::c/border settings))})

(defn- table-header-row [settings child]
  [s/th
   {:style
    (assoc
     (get-styles settings)
     :top 0
     :z-index 2
     :position :sticky
     :background (ins/get-background settings))}
   child])

(defn- table-header-column [settings child]
  [s/th
   {:style
    (assoc
     (get-styles settings)
     :left 0
     :z-index 1
     :position :sticky
     :text-align :right
     :background (ins/get-background settings))}
   child])

(defn- table-data [settings child]
  [s/td {:style (get-styles settings)} child])

(defn- table [settings component rows cols]
  (let [transpose? false]
    [s/table
     {:style
      {:width "100%"
       :border-top (str "1px solid " (::c/border settings))
       :border-left (str "1px solid " (::c/border settings))
       :background (ins/get-background settings)
       :border-spacing 0
       :position :relative
       :color (::c/text settings)
       :font-size  (:font-size settings)
       :border-radius (:border-radius settings)}}
     [s/tbody
      [l/lazy-seq
       (map-indexed
        (fn [row-index row]
          [s/tr
           {:key row-index
            :style/hover
            {:background (str (::c/border settings) "55")}}
           (map-indexed
            (fn [col-index col]
              ^{:key col-index}
              [component
               (if-not transpose?
                 {:row row    :row-index row-index
                  :column col :column-index col-index}
                 {:row col    :row-index col-index
                  :column row :column-index row-index})])
            (if transpose? rows cols))])
        (if transpose? cols rows))]]]))

(defn- inspect-map-table [settings values]
  (let [columns (into #{} (mapcat keys (vals values)))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= column row ::header) [table-header-row settings]

           (= row ::header)
           [table-header-row
            settings
            [inspector (assoc settings :coll values) column]]

           (= column ::header)
           [table-header-column
            settings
            [inspector (assoc settings :coll values) (first row)]]

           :else
           [table-data
            settings
            (let [[_ row] row]
              (when (contains? row column)
                [inspector (assoc settings :coll row :k column) (get row column)]))])))
     (concat [::header] values)
     (concat [::header] columns)]))

(defn- inspect-coll-table [settings values]
  (let [columns (into #{} (mapcat keys values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= column row ::header) [table-header-row settings]

           (= row ::header)
           [table-header-row
            settings
            [inspector (assoc settings :coll values) column]]

           (= column ::header)
           [table-header-column
            settings
            [inspector (assoc settings :coll values) (dec (:row-index context))]]

           :else
           [table-data
            settings
            (when (contains? row column)
              [inspector (assoc settings :coll row :k column) (get row column)])])))
     (concat [::header] values)
     (concat [::header] columns)]))

(defn- inspect-vector-table [settings values]
  (let [n (reduce max (map count values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         [table-data
          settings
          (when (< column (count row))
            [inspector (assoc settings :coll row :k column) (get row column)])]))
     values
     (range n)]))

(defn- inspect-set-table [settings values]
  (let [columns (into #{} (mapcat keys values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= row ::header)
           [table-header-row
            settings
            [inspector (assoc settings :coll row) column]]

           (contains? column row)
           [table-data
            settings
            [inspector (assoc settings :coll row :k column) (get row column)]])))
     (concat [::header] values)
     columns]))

(defn- get-component [value]
  (cond
    (and (or (vector? value) (list? value))
         (every? vector? value))
    inspect-vector-table

    (and (map? value) (every? map? (vals value)))
    inspect-map-table

    (and (set? value) (every? map? value))
    inspect-set-table

    (and (coll? value) (every? map? value))
    inspect-coll-table))

(defn table-view? [value] (some? (get-component value)))

(defn inspect-table [settings values]
  (let [component (get-component values)]
    [component settings values]))
