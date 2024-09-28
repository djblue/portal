(ns portal.ui.icons
  (:require ["@fortawesome/free-solid-svg-icons/faArrowDown" :refer [faArrowDown]]
            ["@fortawesome/free-solid-svg-icons/faArrowLeft" :refer [faArrowLeft]]
            ["@fortawesome/free-solid-svg-icons/faArrowRight" :refer [faArrowRight]]
            ["@fortawesome/free-solid-svg-icons/faArrowUp" :refer [faArrowUp]]
            ["@fortawesome/free-solid-svg-icons/faAt" :refer [faAt]]
            ["@fortawesome/free-solid-svg-icons/faBan" :refer [faBan]]
            ["@fortawesome/free-solid-svg-icons/faCaretDown" :refer [faCaretDown]]
            ["@fortawesome/free-solid-svg-icons/faCaretLeft" :refer [faCaretLeft]]
            ["@fortawesome/free-solid-svg-icons/faCaretRight" :refer [faCaretRight]]
            ["@fortawesome/free-solid-svg-icons/faCaretUp" :refer [faCaretUp]]
            ["@fortawesome/free-solid-svg-icons/faCheckCircle" :refer [faCheckCircle]]
            ["@fortawesome/free-solid-svg-icons/faChevronDown" :refer [faChevronDown]]
            ["@fortawesome/free-solid-svg-icons/faChevronRight" :refer [faChevronRight]]
            ["@fortawesome/free-solid-svg-icons/faCircle" :refer [faCircle]]
            ["@fortawesome/free-solid-svg-icons/faCopy" :refer [faCopy]]
            ["@fortawesome/free-solid-svg-icons/faEllipsisH" :refer [faEllipsisH]]
            ["@fortawesome/free-solid-svg-icons/faExchangeAlt" :refer [faExchangeAlt]]
            ["@fortawesome/free-solid-svg-icons/faExclamationTriangle" :refer [faExclamationTriangle]]
            ["@fortawesome/free-solid-svg-icons/faExternalLinkAlt" :refer [faExternalLinkAlt]]
            ["@fortawesome/free-solid-svg-icons/faFileCode" :refer [faFileCode]]
            ["@fortawesome/free-solid-svg-icons/faInfoCircle" :refer [faInfoCircle]]
            ["@fortawesome/free-solid-svg-icons/faMinusCircle" :refer [faMinusCircle]]
            ["@fortawesome/free-solid-svg-icons/faPause" :refer [faPause]]
            ["@fortawesome/free-solid-svg-icons/faPlay" :refer [faPlay]]
            ["@fortawesome/free-solid-svg-icons/faPlayCircle" :refer [faPlayCircle]]
            ["@fortawesome/free-solid-svg-icons/faPlusCircle" :refer [faPlusCircle]]
            ["@fortawesome/free-solid-svg-icons/faSignOutAlt" :refer [faSignOutAlt]]
            ["@fortawesome/free-solid-svg-icons/faStopCircle" :refer [faStopCircle]]
            ["@fortawesome/free-solid-svg-icons/faTerminal" :refer [faTerminal]]
            ["@fortawesome/free-solid-svg-icons/faTimes" :refer [faTimes]]
            ["@fortawesome/free-solid-svg-icons/faTimesCircle" :refer [faTimesCircle]]
            ["@fortawesome/react-fontawesome" :refer [FontAwesomeIcon]]
            [portal.ui.styled :as d]))

(defn icon [icon props]
  [:> FontAwesomeIcon (d/attrs->css (merge {:icon icon :size "lg"} props))])

(def arrow-down (partial icon faArrowDown))
(def arrow-left (partial icon faArrowLeft))
(def arrow-right (partial icon faArrowRight))
(def arrow-up (partial icon faArrowUp))
(def at (partial icon faAt))
(def ban (partial icon faBan))
(def caret-down (partial icon faCaretDown))
(def caret-left (partial icon faCaretLeft))
(def caret-right (partial icon faCaretRight))
(def caret-up (partial icon faCaretUp))
(def check-circle (partial icon faCheckCircle))
(def chevron-down (partial icon faChevronDown))
(def chevron-right (partial icon faChevronRight))
(def circle (partial icon faCircle))
(def copy (partial icon faCopy))
(def ellipsis-h (partial icon faEllipsisH))
(def exchange-alt (partial icon faExchangeAlt))
(def exclamation-triangle (partial icon faExclamationTriangle))
(def external-link (partial icon faExternalLinkAlt))
(def file-code (partial icon faFileCode))
(def info-circle (partial icon faInfoCircle))
(def minus-circle (partial icon faMinusCircle))
(def pause (partial icon faPause))
(def play-circle (partial icon faPlayCircle))
(def play (partial icon faPlay))
(def plus-circle (partial icon faPlusCircle))
(def sign-out-alt (partial icon faSignOutAlt))
(def stop-circle (partial icon faStopCircle))
(def terminal (partial icon faTerminal))
(def times-circle (partial icon faTimesCircle))
(def times (partial icon faTimes))