(ns ^:no-doc portal.ui.viewer.deref
  (:require [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as options]
            [portal.ui.react :as react]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

(defn atom? [value]
  (and (satisfies? cljs.core/IDeref value)
       (not (instance? cljs.core/Var value))))

(defn- toggle-watch [value]
  (let [theme  (theme/use-theme)
        opts   (options/use-options)
        value' @value
        deref? (some-> opts :watch-registry deref (contains? value))
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
       :color      (if deref?
                     (::c/tag theme)
                     (::c/exception theme))
       :font-size  (:font-size theme)
       :box-sizing :border-box
       :padding    (if-not (coll? value')
                     0
                     (inc (:padding theme)))}

      :style/hover {:opacity 1}

      :on-mouse-enter (fn [_e] (set-hover! true))
      :on-mouse-leave (fn [_e] (set-hover! false))}
     (when hover
       (if deref?
         [icons/pause
          {:title "Pause watch."
           :on-click
           (fn [e]
             (.stopPropagation e)
             (rpc/call 'portal.api/toggle-watch value))}]
         [icons/play
          {:title "Resume watch."
           :on-click
           (fn [e]
             (.stopPropagation e)
             (rpc/call 'portal.api/toggle-watch value))}]))
     [icons/at
      {:title (str "Click to select atom. "
                   (when-not deref?
                     "(watch paused)"))}]]))

(defn inspect-deref [value]
  [d/div
   {:style {:position :relative}}
   [toggle-watch value]
   [select/with-position
    {:row 0 :column 0}
    [ins/toggle-bg
     [ins/dec-depth [ins/inspector @value]]]]])

(def viewer
  {:predicate atom?
   :component #'inspect-deref
   :name :portal.viewer/deref})
