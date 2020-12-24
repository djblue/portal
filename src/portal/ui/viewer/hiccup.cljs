(ns portal.ui.viewer.hiccup
  (:require [clojure.walk :as w]
            [portal.colors :as c]))

(defn header-styles [settings]
  {:color (::c/namespace settings)
   :padding-top (:spacing/padding settings)
   :padding-bottom (:spacing/padding settings)
   :margin-bottom (* 2 (:spacing/padding settings))})

(defn hiccup-styles [settings]
  (let [h (header-styles settings)
        border-bottom
        {:border-bottom
         (str "1px solid " (::c/border settings))}]
    {:h1 (merge h border-bottom)
     :h2 (merge h border-bottom)

     :h3 h :h4 h :h5 h :h6 h

     :a {:color (::c/uri settings)}

     :p {:font-family "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji"
         :font-size (:font-size settings)
         :line-height 1.5
         :margin-top 0
         :margin-bottom (* 2 (:spacing/padding settings))}

     :img {:max-width "100%"}

     :code
     {:background (::c/background2 settings)
      :border-radius (:border-radius settings)}

     :pre
     {:overflow :auto
      :padding (* 2 (:spacing/padding settings))
      :background (::c/background2 settings)
      :border-radius (:border-radius settings)}

     :table {:color (::c/text settings)
             :width "100%"
             :overflow :auto
             :border-spacing 0
             :border-collapse :collapse}
     :th {:padding (:spacing/padding settings)
          :border (str "1px solid " (::c/border settings))}
     :td {:padding (:spacing/padding settings)
          :border (str "1px solid " (::c/border settings))}}))

(defn inspect-hiccup [settings value]
  (let [styles (hiccup-styles settings)]
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
