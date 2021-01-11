(ns portal.ui.viewer.image
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins]
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
       :border [1 :solid (::c/border settings)]}}]))

(def viewer
  {:predicate ins/bin?
   :component inspect-image
   :name :portal.viewer/image})
