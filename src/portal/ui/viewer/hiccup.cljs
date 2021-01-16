(ns portal.ui.viewer.hiccup
  (:require [clojure.walk :as w]
            [portal.colors :as c]
            [portal.ui.theme :as theme]))

(defn header-styles [theme]
  {:color (::c/namespace theme)
   :padding-top (:spacing/padding theme)
   :padding-bottom (:spacing/padding theme)
   :margin-bottom (* 2 (:spacing/padding theme))})

(defn hiccup-styles [theme]
  (let [h (header-styles theme)
        border-bottom
        {:border-bottom
         (str "1px solid " (::c/border theme))}]
    {:h1 (merge h border-bottom)
     :h2 (merge h border-bottom)

     :h3 h :h4 h :h5 h :h6 h

     :a {:color (::c/uri theme)}

     :p {:font-family "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji"
         :font-size (:font-size theme)
         :line-height 1.5
         :margin-top 0
         :margin-bottom (* 2 (:spacing/padding theme))}

     :img {:max-width "100%"}

     :code
     {:background (::c/background2 theme)
      :border-radius (:border-radius theme)}

     :pre
     {:overflow :auto
      :padding (* 2 (:spacing/padding theme))
      :background (::c/background2 theme)
      :border-radius (:border-radius theme)}

     :table {:color (::c/text theme)
             :width "100%"
             :overflow :auto
             :border-spacing 0
             :border-collapse :collapse}
     :th {:padding (:spacing/padding theme)
          :border [1 :solid (::c/border theme)]}
     :td {:padding (:spacing/padding theme)
          :border [1 :solid (::c/border theme)]}}))

(defn inspect-hiccup [_settings value]
  (let [theme (theme/use-theme)
        styles (hiccup-styles theme)]
    (w/postwalk
     (fn [x]
       (let [style (and (vector? x)
                        (get styles (first x)))]
         (if-not style
           x
           (update-in
            (if (map? (second x))
              x
              (into [(first x) {}] (rest x)))
            [1 :style]
            #(merge style %)))))
     value)))

(def viewer
  {:predicate vector?
   :component inspect-hiccup
   :name :portal.viewer/hiccup})
