(ns portal.viewer.text
  (:require [portal.colors :as c]
            [portal.inspector :as ins]))

(defn inspect-text [settings value]
  [:pre
   {:style
    {:cursor :text
     :overflow :auto
     :padding (:spacing/padding settings)
     :background (ins/get-background settings)
     :border-radius (:border-radius settings)
     :border (str "1px solid " (::c/border settings))}}
   value])

