(ns portal.ui.icons
  (:require ["@fortawesome/free-solid-svg-icons/faArrowLeft" :as left]
            ["@fortawesome/free-solid-svg-icons/faArrowRight" :as right]
            ["@fortawesome/free-solid-svg-icons/faChevronDown" :as chevron-down]
            ["@fortawesome/free-solid-svg-icons/faChevronRight" :as chevron-right]
            ["@fortawesome/free-solid-svg-icons/faCopy" :as copy]
            ["@fortawesome/free-solid-svg-icons/faTerminal" :as terminal]
            ["@fortawesome/react-fontawesome" :as react-fontawesome]))

(defn icon [icon props]
  [:> react-fontawesome/FontAwesomeIcon (merge {:icon icon :size "lg"} props)])

(def arrow-left    (partial icon left/faArrowLeft))
(def arrow-right   (partial icon right/faArrowRight))
(def chevron-down  (partial icon chevron-down/faChevronDown))
(def chevron-right (partial icon chevron-right/faChevronRight))
(def copy          (partial icon copy/faCopy))
(def terminal      (partial icon terminal/faTerminal))
