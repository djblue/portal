(ns portal.ui.icons
  (:require ["@fortawesome/free-solid-svg-icons/faArrowDown"        :refer [faArrowDown]]
            ["@fortawesome/free-solid-svg-icons/faArrowLeft"        :refer [faArrowLeft]]
            ["@fortawesome/free-solid-svg-icons/faArrowRight"       :refer [faArrowRight]]
            ["@fortawesome/free-solid-svg-icons/faArrowUp"          :refer [faArrowUp]]
            ["@fortawesome/free-solid-svg-icons/faCheckCircle"      :refer [faCheckCircle]]
            ["@fortawesome/free-solid-svg-icons/faChevronDown"      :refer [faChevronDown]]
            ["@fortawesome/free-solid-svg-icons/faChevronRight"     :refer [faChevronRight]]
            ["@fortawesome/free-solid-svg-icons/faCircle"           :refer [faCircle]]
            ["@fortawesome/free-solid-svg-icons/faCopy"             :refer [faCopy]]
            ["@fortawesome/free-solid-svg-icons/faEllipsisH"        :refer [faEllipsisH]]
            ["@fortawesome/free-solid-svg-icons/faExternalLinkAlt"  :refer [faExternalLinkAlt]]
            ["@fortawesome/free-solid-svg-icons/faInfoCircle"       :refer [faInfoCircle]]
            ["@fortawesome/free-solid-svg-icons/faBan"              :refer [faBan]]
            ["@fortawesome/free-solid-svg-icons/faPlayCircle"       :refer [faPlayCircle]]
            ["@fortawesome/free-solid-svg-icons/faStopCircle"       :refer [faStopCircle]]
            ["@fortawesome/free-solid-svg-icons/faTerminal"         :refer [faTerminal]]
            ["@fortawesome/free-solid-svg-icons/faTimesCircle"      :refer [faTimesCircle]]
            ["@fortawesome/react-fontawesome"                       :refer [FontAwesomeIcon]]))

(defn icon [icon props]
  [:> FontAwesomeIcon (merge {:icon icon :size "lg"} props)])

(def arrow-down    (partial icon faArrowDown))
(def arrow-left    (partial icon faArrowLeft))
(def arrow-right   (partial icon faArrowRight))
(def arrow-up      (partial icon faArrowUp))
(def ban           (partial icon faBan))
(def check-circle  (partial icon faCheckCircle))
(def chevron-down  (partial icon faChevronDown))
(def chevron-right (partial icon faChevronRight))
(def circle        (partial icon faCircle))
(def copy          (partial icon faCopy))
(def ellipsis-h    (partial icon faEllipsisH))
(def external-link (partial icon faExternalLinkAlt))
(def info-circle   (partial icon faInfoCircle))
(def play-circle   (partial icon faPlayCircle))
(def stop-circle   (partial icon faStopCircle))
(def terminal      (partial icon faTerminal))
(def times-circle  (partial icon faTimesCircle))
