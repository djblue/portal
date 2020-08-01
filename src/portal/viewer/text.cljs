(ns portal.viewer.text
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.inspector :as ins]
            [portal.lazy :as l]
            [portal.styled :as s]))

(defn inspect-text [settings value]
  [s/div
   {:style
    {:overflow :auto
     :background (ins/get-background settings)
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :cursor :text
     :border-radius (:border-radius settings)
     :border (str "1px solid " (::c/border settings))}}
   [s/table
    [s/tbody
     [l/lazy-seq
      (map-indexed
       (fn [line line-content]
         [s/tr
          {:key line}
          [s/td
           {:style
            {:color (::c/number settings)
             :font-size (:font-size settings)
             :user-select :none
             :text-align :right
             :padding-right (* 2 (:spacing/padding settings))}}
           [:span (inc line)]]
          [s/td
           {:style
            {:color (::c/text settings)
             :font-size (:font-size settings)}}
           [:pre line-content]]])
       (str/split value #"\n"))
      {:default-take 100 :step 100}]]]])

