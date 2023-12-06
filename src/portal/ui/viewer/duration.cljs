(ns ^:no-doc portal.ui.viewer.duration
  (:require
   [portal.colors :as-alias c]
   [portal.ui.inspector :as ins]
   [portal.ui.select :as select]
   [portal.ui.styled :as d]
   [portal.ui.theme :as theme]))

(defn- round
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(def ^:private unit->color
  {:w  {:color ::c/string :title "week"}
   :d  {:color ::c/package :title "day"}
   :h  {:color ::c/tag :title "hour"}
   :m  {:color ::c/exception :title "minue"}
   :s  {:color ::c/uri :title "second"}
   :ms {:color ::c/string :title "millisecond"}
   :μs {:color ::c/package :title "microsecond"}
   :ns {:color ::c/tag :title "nanosecond"}})

(defn- view [scalar unit]
  (let [theme (theme/use-theme)
        bg    (ins/get-background)
        color (::c/border theme)
        info  (unit->color unit)
        unit-color (get theme (:color info))]
    [d/div
     {:style
      {:display :flex
       :align-items :stretch
       :width :fit-content}}
     [d/div
      {:style
       {:background bg
        :padding-left (* 0.5 (:padding theme))
        :padding-right (* 0.5 (:padding theme))
        :border-top [1 :solid color]
        :border-bottom [1 :solid color]
        :border-left [1 :solid color]
        :border-top-left-radius (:border-radius theme)
        :border-bottom-left-radius (:border-radius theme)}}
      [select/with-position
       {:row 0 :column 0}
       [ins/with-key
        unit
        [ins/inspector (round 2 scalar)]]]]
     [d/div
      {:title (:title info)
       :style
       {:display :flex
        :font-weight :bold
        :color (ins/get-background)
        :background unit-color
        :border-top [1 :solid unit-color]
        :border-bottom [1 :solid unit-color]
        :border-right [1 :solid unit-color]
        :border-top-right-radius (:border-radius theme)
        :border-bottom-right-radius (:border-radius theme)
        :padding-left (* 0.5 (:padding theme))
        :padding-right (* 0.5 (:padding theme))}}
      (name unit)]]))

(defn inspect-nano [ns]
  (cond
    (>= ns 6e10) [view (/ ns 6e10) :m]
    (>= ns 1e9)  [view (/ ns 1e9)  :s]
    (>= ns 1e6)  [view (/ ns 1e6)  :ms]
    (>= ns 1e3)  [view (/ ns 1e3)  :μs]
    :else        [view ns          :ns]))

(def nano
  {:predicate number?
   :name :portal.viewer/duration-ns
   :component inspect-nano
   :doc "Interpret number as a duration in nanoseconds, round up to minutes."})

(defn inspect-ms [ms]
  (cond
    (>= ms 6048e5) [view (/ ms 6048e5) :w]
    (>= ms 864e5)  [view (/ ms 864e5)  :d]
    (>= ms 36e5)   [view (/ ms 36e5)   :h]
    (>= ms 6e4)    [view (/ ms 6e4)    :m]
    (>= ms 1e3)    [view (/ ms 1e3)    :s]
    :else          [view ms            :ms]))

(def ms
  {:predicate number?
   :name :portal.viewer/duration-ms
   :component inspect-ms
   :doc "Interpret number as a duration in milliseconds, round up to minutes."})
