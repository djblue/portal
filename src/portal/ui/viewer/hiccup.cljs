(ns portal.ui.viewer.hiccup
  (:require [clojure.walk :as w]
            [portal.colors :as c]))

(defn headers [settings form]
  (update-in
   form
   [1 :style]
   assoc
   :color
   (::c/namespace settings)
   :padding-top
   (* 0.5 (:spacing/padding settings))
   :padding-bottom
   (* 0.5 (:spacing/padding settings))
   :margin-bottom
   (:spacing/padding settings)
   :border-bottom
   (str "1px solid " (::c/border settings))))

(def hiccup-styles
  {:h1 headers :h2 headers :h3 headers
   :h4 headers :h5 headers :h6 headers
   :a
   (fn [settings form]
     (update-in
      form
      [1 :style]
      assoc
      :color
      (::c/uri settings)))
   :p
   (fn [settings form]
     (update-in
      form
      [1 :style]
      assoc
      :font-family "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji"
      :font-size (:font-size settings)))
   :img
   (fn [_settings form]
     (update-in
      form
      [1 :style]
      assoc
      :max-width "100%"))
   :code
   (fn [settings form]
     (update-in
      form
      [1 :style]
      assoc
      :background (::c/background2 settings)
      :border-radius (:border-radius settings)))
   :pre
   (fn [settings form]
     (update-in
      form
      [1 :style]
      assoc
      :overflow :auto
      :padding (* 2 (:spacing/padding settings))
      :background (::c/background2 settings)
      :border-radius (:border-radius settings)))})

(defn inspect-hiccup [settings value]
  (w/postwalk
   (fn [x]
     (let [f (and (vector? x)
                  (get hiccup-styles (first x)))]
       (if-not (fn? f)
         x
         (if (map? (second x))
           (f settings x)
           (f settings (into [(first x) {}] (rest x)))))))
   value))

(def viewer
  {:predicate vector?
   :component inspect-hiccup
   :name :portal.viewer/hiccup})
