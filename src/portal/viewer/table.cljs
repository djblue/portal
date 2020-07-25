(ns portal.viewer.table
  (:require [portal.colors :as c]
            [portal.inspector :as ins :refer [inspector]]
            [portal.styled :as s]))

(defn table-view? [value]
  (and (coll? value) (every? map? value)))

(defn inspect-table [settings values]
  (let [columns (into #{} (mapcat keys values))
        background (ins/get-background settings)]
    [s/table
     {:style
      {:width "100%"
       :border-collapse :collapse
       :color (::c/text settings)
       :font-size  (:font-size settings)
       :border-radius (:border-radius settings)}}
     [s/tbody
      [s/tr
       (map-indexed
        (fn [grid-column column]
          [s/th {:key grid-column
                 :style
                 {:border (str "1px solid " (::c/border settings))
                  :background background
                  :box-sizing :border-box
                  :padding (:spacing/padding settings)}}
           [inspector (assoc settings :coll values) column]])
        columns)]
      (map-indexed
       (fn [grid-row row]
         [s/tr {:key grid-row}
          (map-indexed
           (fn [grid-column column]
             [s/td
              {:key grid-column
               :style
               {:border (str "1px solid " (::c/border settings))
                :background background
                :padding (:spacing/padding settings)
                :box-sizing :border-box}}
              (when (contains? row column)
                [inspector
                 (assoc settings :coll row :k column)
                 (get row column)])])
           columns)])
       values)]]))
