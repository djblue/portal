(ns portal.viewer.table
  (:require [portal.colors :as c]
            [portal.inspector :as ins :refer [inspector]]
            [portal.lazy :as l]
            [portal.styled :as s]))

(defn- get-styles [settings]
  {:border (str "1px solid " (::c/border settings))
   :background (ins/get-background settings)
   :box-sizing :border-box
   :padding (:spacing/padding settings)})

(defn- table [settings component rows cols]
  (let [transpose? false]
    [s/table
     {:style
      {:width "100%"
       :border-collapse :collapse
       :color (::c/text settings)
       :font-size  (:font-size settings)
       :border-radius (:border-radius settings)}}
     [s/tbody
      [l/lazy-seq
       (map-indexed
        (fn [row-index row]
          [s/tr
           {:key row-index}
           (map-indexed
            (fn [col-index col]
              [s/td
               {:key col-index :style (get-styles settings)}
               [component
                (if-not transpose?
                  {:row row :column col}
                  {:row col :column row})]])
            (if transpose? rows cols))])
        (if transpose? cols rows))]]]))

(defn- inspect-map-table [settings values]
  (let [columns (into #{} (mapcat (fn [[_ v]] (keys v)) values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (cond
           (= column row ::header) nil

           (= row ::header)
           [inspector (assoc settings :coll values) column]

           (= column ::header)
           [inspector (assoc settings :coll values) (first row)]

           :else
           (let [[_ row] row]
             (when (contains? row column)
               [inspector (assoc settings :coll row :k column) (get row column)])))))
     (into [::header] values)
     (into [::header] columns)]))

(defn- inspect-coll-table [settings values]
  [inspect-map-table settings (zipmap (range) values)])

(defn inspect-vector-table [settings values]
  (let [n (reduce max (map count values))]
    [table
     settings
     (fn [context]
       (let [{:keys [row column]} context]
         (when (< column (count row))
           [inspector (assoc settings :coll row :k column) (get row column)])))
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
           [inspector (assoc settings :coll row) column]

           (contains? column row)
           [inspector (assoc settings :coll row :k column) (get row column)])))
     (into [::header] values)
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
