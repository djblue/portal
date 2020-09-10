(ns portal.ui.viewer.image
  (:require [portal.colors :as c]
            [portal.ui.styled :as s]))

(defn inspect-image [settings value]
  (let [blob (js/Blob. #js [value])
        url  (or js/window.URL js/window.webkitURL)
        src  (.createObjectURL url blob)]
    [s/img
     {:src src
      :style
      {:max-height "100%"
       :max-width "100%"
       :user-select :none
       :border-radius (:border-radius settings)
       :border (str "1px solid " (::c/border settings))}}]))

