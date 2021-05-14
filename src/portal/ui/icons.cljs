(ns portal.ui.icons
  (:require ["@fortawesome/react-fontawesome" :as react-fontawesome]
            ["@fortawesome/free-solid-svg-icons/faCopy" :as copy]))

(defn icon [icon]
  [:> react-fontawesome/FontAwesomeIcon {:icon icon :size "lg"}])

(def fa-copy (partial icon copy/faCopy))
