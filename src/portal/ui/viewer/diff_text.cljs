(ns ^:no-doc portal.ui.viewer.diff-text
  (:require ["diff" :as df]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::diff-text (s/cat :a string? :b string?))
;;;

(defn- diff-text? [value]
  (s/valid? ::diff-text value))

(defn- changed? [^js item]
  (or (some-> item .-added)
      (some-> item .-removed)))

(defn- inspect-text-diff [value]
  (let [theme  (theme/use-theme)
        add    (::c/diff-add theme)
        remove (::c/diff-remove theme)
        diff   (df/diffLines (or (:- value) (first value))
                             (or (:+ value) (second value)))
        opts        (ins/use-options)
        bg          (ins/get-background)]
    [:pre
     {:style {:margin 0 :white-space :pre-wrap :background bg}}
     [d/div
      {:style {:height (:padding theme)
               :padding-left (:padding theme)
               :border-top [1 :solid (::c/border theme)]
               :border-left [5 :solid (::c/border theme)]
               :border-right [1 :solid (::c/border theme)]
               :border-top-left-radius (:border-radius theme)
               :border-top-right-radius (:border-radius theme)}}]
     [l/lazy-seq
      (map-indexed
       (fn [index [before ^js item after]]
         (let [added   (.-added item)
               removed (.-removed item)
               text    (.-value item)
               border-color (cond
                              added add
                              removed remove
                              :else (::c/border theme))]
           ^{:key index}
           [d/div
            {:style
             {:position :relative
              :border-left [5 :solid border-color]
              :border-right [1 :solid border-color]
              :background  (cond added (str add "22")
                                 removed (str remove "22"))}}
            (if-not (or removed added)
              (if (:expanded? opts)
                [ins/highlight-words text]
                (let [lines (str/split-lines text)]
                  (if (< (count lines) 6)
                    [ins/highlight-words text]
                    [:<>
                     (when before
                       [:<>
                        [ins/highlight-words (str/join "\n" (take 3 lines))]
                        [d/div {:style {:background (::c/border theme)
                                        :text-align :center}}
                         [icons/ellipsis-h]]])
                     (when after
                       [:<>
                        [d/div {:style {:background (::c/border theme)
                                        :text-align :center}}
                         [icons/ellipsis-h]]
                        [ins/highlight-words (str/join "\n" (take-last 3 lines))]])])))
              (cond
                (changed? before)
                (keep-indexed
                 (fn [idx ^js item]
                   (when (or (.-added item) (not (.-removed item)))
                     ^{:key idx}
                     [d/span {:style {:background (when (.-added item)
                                                    (if added
                                                      (str add "66")
                                                      (str remove "66")))}}
                      [ins/highlight-words (.-value item)]]))
                 (df/diffChars (.-value before) (str/trimr text)))

                (changed? after)
                (keep-indexed
                 (fn [idx ^js item]
                   (when (or (.-added item) (not (.-removed item)))
                     ^{:key idx}
                     [d/span {:style {:background (when (.-added item)
                                                    (if added
                                                      (str add "66")
                                                      (str remove "66")))}}
                      [ins/highlight-words (.-value item)]]))
                 (df/diffChars (.-value after) (str/trimr text)))

                :else
                (str/trimr text)))

            (when (or removed added) "\n")]))
       (partition 3 1 (concat [nil] diff [nil])))]
     [d/div
      {:style {:height (:padding theme)
               :padding-left (:padding theme)
               :border-bottom [1 :solid (::c/border theme)]
               :border-left [5 :solid (::c/border theme)]
               :border-right [1 :solid (::c/border theme)]
               :border-bottom-left-radius (:border-radius theme)
               :border-bottom-right-radius (:border-radius theme)}}]]))

(def viewer
  {:predicate diff-text?
   :component #'inspect-text-diff
   :name :portal.viewer/diff-text
   :doc "Diff two strings."})