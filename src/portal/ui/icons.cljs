(ns portal.ui.icons
  (:require ["@fortawesome/free-solid-svg-icons/faArrowLeft" :as left]
            ["@fortawesome/free-solid-svg-icons/faArrowRight" :as right]
            ["@fortawesome/free-solid-svg-icons/faCopy" :as copy]
            ["@fortawesome/free-solid-svg-icons/faTerminal" :as terminal]
            ["@fortawesome/react-fontawesome" :as react-fontawesome]))

(defn icon [icon]
  [:r> react-fontawesome/FontAwesomeIcon #js {:icon icon :size "lg"}])

(def arrow-left  (partial icon left/faArrowLeft))
(def arrow-right (partial icon right/faArrowRight))
(def copy        (partial icon copy/faCopy))
(def terminal    (partial icon terminal/faTerminal))
