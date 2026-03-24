(ns portal.ui.icons
  (:refer-clojure :exclude [filter])
  #?(:cljs
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
               ["@fortawesome/free-solid-svg-icons/faFilter" :refer [faFilter]]
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
               [portal.ui.styled :as d])))

(defn mount-stylesheet []
  [:<>
   [:link {:rel "stylesheet"
           :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/fontawesome.min.css"
           :integrity "sha512-P9vJUXK+LyvAzj8otTOKzdfF1F3UYVl13+F8Fof8/2QNb8Twd6Vb+VD52I7+87tex9UXxnzPgWA3rH96RExA7A=="
           :crossorigin "anonymous"
           :referrerpolicy "no-referrer"}]
   [:link {:rel "stylesheet"
           :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.15.4/css/solid.min.css"
           :integrity "sha512-tk4nGrLxft4l30r9ETuejLU0a3d7LwMzj0eXjzc16JQj+5U1IeVoCuGLObRDc3+eQMUcEQY1RIDPGvuA7SNQ2w=="
           :crossorigin "anonymous"
           :referrerpolicy "no-referrer"}]])

(defn icon [icon & [props]]
  #?(:clj [:i (merge
               {:class (str "fas " (name icon)
                            (when-let [size (:size props)]
                              (str " fa-" size)))}
               (dissoc props :size))]
     :cljs [:> FontAwesomeIcon (d/attrs->css (merge {:icon icon :size "lg"} props))]))

#?(:cljs (def arrow-down (partial icon faArrowDown)))
#?(:cljs (def arrow-left (partial icon faArrowLeft)))
#?(:cljs (def arrow-right (partial icon faArrowRight)))
#?(:cljs (def arrow-up (partial icon faArrowUp)))
#?(:cljs (def at (partial icon faAt)))
#?(:cljs (def ban (partial icon faBan)))
#?(:cljs (def caret-down (partial icon faCaretDown)))
#?(:cljs (def caret-left (partial icon faCaretLeft)))
#?(:cljs (def caret-right (partial icon faCaretRight)))
#?(:cljs (def caret-up (partial icon faCaretUp)))
#?(:cljs (def check-circle (partial icon faCheckCircle)))
#?(:cljs (def chevron-down (partial icon faChevronDown)))
#?(:cljs (def chevron-right (partial icon faChevronRight)))
#?(:cljs (def circle (partial icon faCircle)))
#?(:cljs (def copy (partial icon faCopy)))
#?(:cljs (def ellipsis-h (partial icon faEllipsisH)))
#?(:cljs (def exchange-alt (partial icon faExchangeAlt)))
#?(:cljs (def exclamation-triangle (partial icon faExclamationTriangle)))
#?(:cljs (def external-link (partial icon faExternalLinkAlt)))
#?(:cljs (def file-code (partial icon faFileCode)))
#?(:cljs (def filter (partial icon faFilter)))
#?(:cljs (def info-circle (partial icon faInfoCircle)))
#?(:cljs (def minus-circle (partial icon faMinusCircle)))
#?(:cljs (def pause (partial icon faPause)))
#?(:cljs (def play-circle (partial icon faPlayCircle)))
#?(:cljs (def play (partial icon faPlay)))
#?(:cljs (def plus-circle (partial icon faPlusCircle)))
#?(:cljs (def sign-out-alt (partial icon faSignOutAlt)))
#?(:cljs (def stop-circle (partial icon faStopCircle)))
#?(:cljs (def terminal (partial icon faTerminal)))
#?(:cljs (def times-circle (partial icon faTimesCircle)))
#?(:cljs (def times (partial icon faTimes)))

#?(:clj (def arrow-down (partial icon :fa-arrow-down)))
#?(:clj (def arrow-left (partial icon :fa-arrow-left)))
#?(:clj (def arrow-right (partial icon :fa-arrow-right)))
#?(:clj (def arrow-up (partial icon :fa-arrow-up)))
#?(:clj (def at (partial icon :fa-at)))
#?(:clj (def ban (partial icon :fa-ban)))
#?(:clj (def caret-down (partial icon :fa-caret-down)))
#?(:clj (def caret-left (partial icon :fa-caret-left)))
#?(:clj (def caret-right (partial icon :fa-caret-right)))
#?(:clj (def caret-up (partial icon :fa-caret-up)))
#?(:clj (def check-circle (partial icon :fa-check-circle)))
#?(:clj (def chevron-down (partial icon :fa-chevron-cown)))
#?(:clj (def chevron-right (partial icon :fa-chevron-right)))
#?(:clj (def circle (partial icon :fa-circle)))
#?(:clj (def copy (partial icon :fa-copy)))
#?(:clj (def ellipsis-h (partial icon :fa-ellipsis-h)))
#?(:clj (def exchange-alt (partial icon :fa-exchange-alt)))
#?(:clj (def exclamation-triangle (partial icon :fa-exclamation-triangle)))
#?(:clj (def external-link (partial icon :fa-external-link-alt)))
#?(:clj (def file-code (partial icon :fa-file-code)))
#?(:clj (def filter (partial icon :fa-filter)))
#?(:clj (def info-circle (partial icon :fa-info-circle)))
#?(:clj (def minus-circle (partial icon :fa-minus-circle)))
#?(:clj (def pause (partial icon :fa-pause)))
#?(:clj (def play-circle (partial icon :fa-play-circle)))
#?(:clj (def play (partial icon :fa-play)))
#?(:clj (def plus-circle (partial icon :fa-plus-circle)))
#?(:clj (def sign-out-alt (partial icon :fa-sign-out-alt)))
#?(:clj (def stop-circle (partial icon :fa-stop-circle)))
#?(:clj (def terminal (partial icon :fa-terminal)))
#?(:clj (def times-circle (partial icon :fa-times-circle)))
#?(:clj (def times (partial icon :fa-times)))