(ns portal.ssr.ui.icons
  (:refer-clojure :exclude [filter]))

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
  [:i (merge
       {:class (str "fas " (name icon)
                    (when-let [size (:size props)]
                      (str " fa-" size)))}
       (dissoc props :dize))])

(def arrow-down (partial icon :fa-arrow-down))
(def arrow-left (partial icon :fa-arrow-left))
(def arrow-right (partial icon :fa-arrow-right))
(def arrow-up (partial icon :fa-arrow-up))
(def at (partial icon :fa-at))
(def ban (partial icon :fa-ban))
(def caret-down (partial icon :fa-caret-down))
(def caret-left (partial icon :fa-caret-left))
(def caret-right (partial icon :fa-caret-right))
(def caret-up (partial icon :fa-caret-up))
(def check-circle (partial icon :fa-check-circle))
(def chevron-down (partial icon :fa-chevron-cown))
(def chevron-right (partial icon :fa-chevron-right))
(def circle (partial icon :fa-circle))
(def copy (partial icon :fa-copy))
(def ellipsis-h (partial icon :fa-ellipsis-h))
(def exchange-alt (partial icon :fa-exchange-alt))
(def exclamation-triangle (partial icon :fa-exclamation-triangle))
(def external-link (partial icon :fa-external-link-alt))
(def file-code (partial icon :fa-file-code))
(def filter (partial icon :fa-filter))
(def info-circle (partial icon :fa-info-circle))
(def minus-circle (partial icon :fa-minus-circle))
(def pause (partial icon :fa-pause))
(def play-circle (partial icon :fa-play-circle))
(def play (partial icon :fa-play))
(def plus-circle (partial icon :fa-plus-circle))
(def sign-out-alt (partial icon :fa-sign-out-alt))
(def stop-circle (partial icon :fa-stop-circle))
(def terminal (partial icon :fa-terminal))
(def times-circle (partial icon :fa-times-circle))
(def times (partial icon :fa-times))