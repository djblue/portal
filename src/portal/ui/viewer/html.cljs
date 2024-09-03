(ns ^:no-doc portal.ui.viewer.html
  (:require [portal.colors :as c]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn inspect-html [value]
  (let [theme (theme/use-theme)]
    [s/iframe {:style {:width "100%"
                       :height "75vh"
                       :border-radius (:border-radius theme)
                       :border [1 :solid (::c/border theme)]}
               :src-doc value}]))

(def viewer
  {:predicate string?
   :component #'inspect-html
   :name :portal.viewer/html})
