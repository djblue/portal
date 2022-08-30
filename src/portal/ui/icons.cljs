(ns portal.ui.icons
  (:require ["@fortawesome/free-solid-svg-icons/faArrowDown" :as down]
            ["@fortawesome/free-solid-svg-icons/faArrowLeft" :as left]
            ["@fortawesome/free-solid-svg-icons/faArrowRight" :as right]
            ["@fortawesome/free-solid-svg-icons/faArrowUp" :as up]
            ["@fortawesome/free-solid-svg-icons/faCheckCircle" :as check-circle]
            ["@fortawesome/free-solid-svg-icons/faChevronDown" :as chevron-down]
            ["@fortawesome/free-solid-svg-icons/faChevronRight" :as chevron-right]
            ["@fortawesome/free-solid-svg-icons/faCircle" :as circle]
            ["@fortawesome/free-solid-svg-icons/faCopy" :as copy]
            ["@fortawesome/free-solid-svg-icons/faExternalLinkAlt" :as external-link]
            ["@fortawesome/free-solid-svg-icons/faInfoCircle" :as info-circle]
            ["@fortawesome/free-solid-svg-icons/faPlayCircle" :as play-circle]
            ["@fortawesome/free-solid-svg-icons/faStopCircle" :as stop-circle]
            ["@fortawesome/free-solid-svg-icons/faTerminal" :as terminal]
            ["@fortawesome/free-solid-svg-icons/faTimesCircle" :as times-circle]
            ["@fortawesome/react-fontawesome" :as react-fontawesome]))

(defn icon [icon props]
  [:> react-fontawesome/FontAwesomeIcon (merge {:icon icon :size "lg"} props)])

(def arrow-down    (partial icon down/faArrowDown))
(def arrow-left    (partial icon left/faArrowLeft))
(def arrow-right   (partial icon right/faArrowRight))
(def arrow-up      (partial icon up/faArrowUp))
(def check-circle  (partial icon check-circle/faCheckCircle))
(def chevron-down  (partial icon chevron-down/faChevronDown))
(def chevron-right (partial icon chevron-right/faChevronRight))
(def circle        (partial icon circle/faCircle))
(def copy          (partial icon copy/faCopy))
(def external-link (partial icon external-link/faExternalLinkAlt))
(def info-circle   (partial icon info-circle/faInfoCircle))
(def play-circle   (partial icon play-circle/faPlayCircle))
(def stop-circle   (partial icon stop-circle/faStopCircle))
(def terminal      (partial icon terminal/faTerminal))
(def times-circle  (partial icon times-circle/faTimesCircle))
