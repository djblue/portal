(ns ^:no-doc portal.ui.viewer.color
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(defn- hex-short? [string]
  (re-matches #"#[0-9a-fA-F]{3}gi" string))

(defn- hex-full? [string]
  (re-matches #"#[0-9a-fA-F]{6}" string))

(defn- hex-alpha? [string]
  (re-matches #"#[0-9a-fA-F]{8}" string))

(defn- rgb-color? [string]
  (re-matches #"rgb\(\d+,\d+,\d+\)" string))

(defn- rgba-color? [string]
  (re-matches #"rgba\(.+,.+,.+,.+\)" string))

(s/def ::hex
  (s/or :short hex-short?
        :full  hex-full?
        :alpha hex-alpha?))

(s/def ::rgb
  (s/or :rgb rgb-color? :rgba rgba-color?))

(s/def ::color
  (s/and string? (s/or :hex ::hex :rgb ::rgb)))
;;;

(defn inspect-color [value]
  (let [theme (theme/use-theme)]
    [d/div
     {:style {:display     :flex
              :gap         (:padding theme)
              :align-items :center}}
     [d/div
      {:style {:width         (:font-size theme)
               :height        (:font-size theme)
               :border        [1 :solid (::c/border theme)]
               :background    value
               :border-radius (:border-radius theme)}}]
     [d/div [ins/highlight-words value]]]))

(defn- color? [value] (s/valid? ::color value))

(def viewer
  {:predicate color?
   :component #'inspect-color
   :name :portal.viewer/color
   :doc "View hex / rgb / rgba colors"})