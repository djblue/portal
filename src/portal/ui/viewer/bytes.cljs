(ns ^:no-doc portal.ui.viewer.bytes
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
  {:TB {:color ::c/exception :title "terabytes"}
   :GB {:color ::c/uri :title "gigaytes"}
   :MB {:color ::c/string :title "megabybtes"}
   :KB {:color ::c/package :title "kilobytes"}
   :B  {:color ::c/tag :title "bytes"}})

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

(def ^:private KB 1024)
(def ^:private MB (* KB 1024))
(def ^:private GB (* MB 1024))
(def ^:private TB (* GB 1024))

(defn inspect-bytes [bs]
  (cond
    (>= bs TB) [view (/ bs TB) :TB]
    (>= bs GB) [view (/ bs GB) :GB]
    (>= bs MB) [view (/ bs MB) :MB]
    (>= bs KB) [view (/ bs KB) :KB]
    :else      [view bs        :B]))

(def viewer
  {:predicate number?
   :name :portal.viewer/size-bytes
   :component #'inspect-bytes
   :doc "Interpret number as amount of bytes."})
