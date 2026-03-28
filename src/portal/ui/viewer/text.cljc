(ns ^:no-doc portal.ui.viewer.text
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn inspect-text [value]
  (let [theme       (theme/use-theme)
        opts        (ins/use-options)
        background  (ins/get-background)
        search-text (ins/use-search-text)]
    [s/div
     {:style
      {:overflow :auto
       :background background
       :padding (:padding theme)
       :box-sizing :border-box
       :cursor :text
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]
       :max-height (when-not (:expanded? opts) "24rem")}}
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
            (if search-text
              (some
               #(str/includes? line-content %)
               (str/split search-text #"\s+"))
              true)))
         (map
          (fn [[line line-content]]
            [s/tr
             {:key line}
             [s/td
              {:style
               {:color (::c/number theme)
                :background background
                :font-size (:font-size theme)
                :user-select :none
                :text-align :right
                :vertical-align :top
                :padding-right (* 2 (:padding theme))}}
              [s/span line]]
             [s/td
              {:style
               {:color (::c/text theme)
                :background background
                :text-align :left
                :font-size (:font-size theme)}}
              [:pre {:style {:margin 0 :white-space :pre-wrap}}
               [ins/highlight-words line-content]]]])))
        {:default-take 100 :step 100}]]]]))

(def viewer
  {:predicate string?
   :component #'inspect-text
   :name :portal.viewer/text
   :doc "View string as a text file."})
