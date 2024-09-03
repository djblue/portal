(ns ^:no-doc portal.ui.viewer.image
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn inspect-image [value]
  (let [theme (theme/use-theme)
        blob  (js/Blob. #js [value])
        url   (or js/window.URL js/window.webkitURL)
        src   (.createObjectURL url blob)]
    [s/img
     {:src src
      :style
      {:max-height "100%"
       :max-width "100%"
       :user-select :none
       :background (ins/get-background)
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]}}]))

(def viewer
  {:predicate ins/bin?
   :component #'inspect-image
   :name :portal.viewer/image
   :doc "View a binary value as an image."})
