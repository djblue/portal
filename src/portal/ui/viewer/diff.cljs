(ns ^:no-doc portal.ui.viewer.diff
  (:require [clojure.spec.alpha :as s]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.runtime.cson :as cson]
            [portal.ui.commands :as commands]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

(extend-protocol cson/ToJson
  diff/Deletion
  (to-json* [this buffer] (cson/tag buffer "diff/Deletion" (:- this)))

  diff/Insertion
  (to-json* [this buffer] (cson/tag buffer "diff/Insertion" (:+ this)))

  diff/Mismatch
  (to-json* [this buffer] (cson/tag buffer "diff/Mismatch" ((juxt :- :+) this))))

(defmethod rpc/-read "diff/Deletion"  [_ value] (diff/Deletion. value))
(defmethod rpc/-read "diff/Insertion" [_ value] (diff/Insertion. value))
(defmethod rpc/-read "diff/Mismatch"  [_ [a b]] (diff/Mismatch. a b))

(defn diff? [value]
  (or
   (instance? diff/Deletion value)
   (instance? diff/Insertion value)
   (instance? diff/Mismatch value)))

;;; :spec
(s/def ::diffable (s/coll-of any? :min-count 2))
;;;

(defn- can-view? [value]
  (s/valid? ::diffable value))

(defn- test-actual [value]
  (or (and (= (first value) 'not)
           (coll? (second value))
           (= (first (second value)) '=)
           (rest (second value)))
      value))

(defn- background [color]
  [d/div
   {:style
    {:background color
     :position :absolute
     :top 0
     :bottom 0
     :right 0
     :left 0
     :opacity 0.15
     :z-index -1}}])

(defn- insertion [value]
  (let [theme (theme/use-theme)
        color (::c/diff-add theme)]
    [d/div
     {:style {:flex 1
              :border [1 :solid color]
              :position :relative
              :border-radius (:border-radius theme)}}
     [background color]
     [ins/with-key :+ [ins/inspector value]]]))

(defn- deletion [value]
  (let [theme (theme/use-theme)
        color (::c/diff-remove theme)]
    [d/div
     {:style {:flex 1
              :border [1 :solid color]
              :position :relative
              :border-radius (:border-radius theme)}}
     [background color]
     [ins/with-key :- [ins/inspector value]]]))

(defn- mismatch [value]
  (let [theme (theme/use-theme)
        removed (get value :- ::not-found)
        added   (get value :+ ::not-found)]
    [d/div
     {:style {:display :flex :width "100%"}}
     (when-not (= removed ::not-found)
       [d/div {:style
               {:flex 1
                :margin-right
                (when-not (= added ::not-found)
                  (:padding theme))}}
        [select/with-position {:row 0 :column 0}
         [deletion removed]]])
     (when-not (= added ::not-found)
       [d/div {:style {:flex 1}}
        [select/with-position {:row 0 :column 1}
         [insertion added]]])]))

(defn- inspect-deep-diff2 [value]
  (cond
    (instance? diff/Mismatch value)  [mismatch value]
    (instance? diff/Deletion value)
    [select/with-position {:row 0 :column 0}
     [deletion (:- value)]]
    (instance? diff/Insertion value)
    [select/with-position {:row 0 :column 0}
     [insertion (:+ value)]]))

(defn inspect-diff [value]
  (let [theme (theme/use-theme)
        bg (ins/get-background)]
    [d/div
     {:style
      {:background (ins/get-background)}}
     [d/div
      {:style
       {:display :flex
        :justify-content :space-between}}
      [d/div
       {:style
        {:flex "1"
         :padding (:padding theme)
         :background (::c/diff-remove theme)
         :border-top-left-radius (:border-radius theme)}}
       [icons/minus-circle {:style {:color bg}}]]
      [d/div
       {:style
        {:padding (:padding theme)
         :color (::c/border theme)
         :border [1 :solid (::c/border theme)]}}
       [icons/exchange-alt]]
      [d/div
       {:style
        {:flex "1"
         :padding (:padding theme)
         :text-align :right
         :background (::c/diff-add theme)
         :border-top-right-radius (:border-radius theme)}}
       [icons/plus-circle {:style {:color bg}}]]]
     [d/div
      {:style
       {:display :flex
        :flex-direction :column
        :gap (:padding theme)
        :padding (:padding theme)
        :border-left [1 :solid (::c/border theme)]
        :border-right [1 :solid (::c/border theme)]
        :border-bottom [1 :solid (::c/border theme)]
        :border-bottom-left-radius (:border-radius theme)
        :border-bottom-right-radius (:border-radius theme)}}
      [ins/dec-depth
       (->> (test-actual value)
            (partition 2 1)
            (map-indexed
             (fn [idx [a b]]
               ^{:key idx}
               [ins/with-key
                idx
                [ins/with-collection
                 [a b]
                 [select/with-position
                  {:row idx :column 0}
                  [ins/inspector (diff/diff a b)]]]])))]]]))

(def ^:no-doc deep-diff2
  {:predicate diff?
   :component inspect-deep-diff2
   :name      ::diff/diff})

(def viewer
  {:predicate can-view?
   :component #'inspect-diff
   :name :portal.viewer/diff
   :doc "Diff a collection of values successively starting with the first two."})

(let [var  #'diff/diff
      name (#'commands/var->name var)]
  (swap! commands/registry
         assoc name (commands/make-command
                     (merge (meta var)
                            {:predicate (fn [& args] (= 2 (count args)))
                             :f var
                             :name name}))))
