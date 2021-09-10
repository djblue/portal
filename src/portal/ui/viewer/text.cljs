(ns portal.ui.viewer.text
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn inspect-text [value]
  (let [theme    (theme/use-theme)
        state    (state/use-state)
        context  (ins/use-context)
        location (state/get-location context)]
    [s/div
     {:style
      {:overflow :auto
       :background (::c/background2 theme)
       :padding (:spacing/padding theme)
       :box-sizing :border-box
       :cursor :text
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]}}
     [s/table
      [s/tbody
       [l/lazy-seq
        (->>
         (str/split value #"\n")
         (map-indexed
          (fn [line line-content]
            [(inc line) line-content]))
         (filter
          (fn [[_ line-content]]
            (if-let [search-text (get-in @state [:search-text location])]
              (str/includes? line-content search-text)
              true)))
         (map
          (fn [[line line-content]]
            [s/tr
             {:key line}
             [s/td
              {:style
               {:color (::c/number theme)
                :font-size (:font-size theme)
                :user-select :none
                :text-align :right
                :padding-right (* 2 (:spacing/padding theme))}}
              [s/span line]]
             [s/td
              {:style
               {:color (::c/text theme)
                :font-size (:font-size theme)}}
              [:pre {:style {:margin 0}} line-content]]])))
        {:default-take 100 :step 100}]]]]))

(def viewer
  {:predicate string?
   :component inspect-text
   :name :portal.viewer/text})
