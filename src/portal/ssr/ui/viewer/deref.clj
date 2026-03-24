(ns ^:no-doc portal.ssr.ui.viewer.deref
  (:require [portal.colors :as c]
            [portal.ssr.ui.icons :as icons]
            [portal.ssr.ui.inspector :as ins]
            [portal.ssr.ui.react :as react]
            [portal.ui.select :as select]
            [portal.ssr.ui.state :refer [atom?]]
            [portal.ui.styled :as d]
            [portal.ssr.ui.theme :as theme]))

(defn- toggle-watch [value' active? set-active!]
  (let [theme  (theme/use-theme)
        [hover set-hover!] (react/use-state false)]
    [d/div
     {:style
      {:top        0
       :right      0
       :z-index    100
       :opacity    0.5
       :display    :flex
       :gap        (:padding theme)
       :position   :absolute
       :cursor     :pointer
       :color      (if active?
                     (::c/tag theme)
                     (::c/exception theme))
       :font-size  "1.4rem"
       :box-sizing :border-box
       :padding    (if-not (coll? value')
                     0
                     (inc (:padding theme)))}

      :style/hover {:opacity 1}

      :on-mouse-enter (fn [_] (set-hover! true))
      :on-mouse-leave (fn [_] (set-hover! false))}
     (if active?
       [icons/pause
        {:title "Pause watch."
         :style {:opacity (if hover 1 0)}
         :on-click
         (fn [_]
           (set-active! false))}]
       [icons/play
        {:title "Resume watch."
         :style {:opacity (if hover 1 0)}
         :on-click
         (fn [_]
           (set-active! true))}])
     [icons/at
      {:title (str "Click to select atom. "
                   (when-not active?
                     "(watch paused)"))}]]))

(defn inspect-deref [value]
  (let [[active? set-active!] (react/use-state true)
        value' (react/use-atom value identity active?)]
    [d/div
     {:style {:position :relative}}
     [toggle-watch value' active? set-active!]
     [select/with-position
      {:row 0 :column 0}
      [ins/toggle-bg
       [ins/dec-depth [ins/inspector value']]]]]))

(def viewer
  {:predicate atom?
   :component #'inspect-deref
   :name :portal.viewer/deref})

