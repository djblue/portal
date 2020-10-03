(ns portal.ui.viewer.html
  (:require [portal.colors :as c]))

(defn inspect-html [settings value]
  [:iframe {:style {:width "100%"
                    :height "100%"
                    :border-radius (:border-radius settings)
                    :border (str "1px solid " (::c/border settings))}
            :src-doc value}])

(def viewer
  {:predicate string?
   :component inspect-html
   :name :portal.viewer/html})
