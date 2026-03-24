(ns ^:no-doc portal.ui.viewer.deref
  (:require [portal.colors :as c]
            [portal.ui.icons :as icons]
            #?(:clj  [portal.ssr.ui.inspector :as ins]
               :cljs [portal.ui.inspector :as ins])
            #?(:cljs [portal.ui.options :as options])
            [portal.ui.react :as react]
            #?(:cljs [portal.ui.rpc :as rpc])
            [portal.ui.select :as select]
            #?(:clj  [portal.ssr.ui.state :refer [atom?]]
               :cljs [portal.ui.state :refer [atom?]])
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

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
       :font-size  #?(:clj "1.35rem"
                      :cljs  (:font-size theme))
       :box-sizing :border-box
       :padding    (if-not (coll? value')
                     0
                     (inc (:padding theme)))}

      :style/hover {:opacity 1}

      :on-mouse-enter (fn [_e] (set-hover! true))
      :on-mouse-leave (fn [_e] (set-hover! false))}
     (if active?
       [icons/pause
        {:title "Pause watch."
         :style {:opacity (if hover 1 0)}
         :on-click
         (fn [_e]
           (set-active! false)
           #?(:cljs (.stopPropagation _e)))}]
       [icons/play
        {:title "Resume watch."
         :style {:opacity (if hover 1 0)}
         :on-click
         (fn [_e]
           (set-active! true)
           #?(:cljs (.stopPropagation _e)))}])
     [icons/at
      {:title (str "Click to select atom. "
                   (when-not active?
                     "(watch paused)"))}]]))

(defn- use-watch [_value]
  #?(:clj  (react/use-state true)
     :cljs (let [opts    (options/use-options)
                 active? (some-> opts :watch-registry deref (contains? _value))]
             [active? (fn set-active! [_]
                        (prn :set-active)
                        (rpc/call 'portal.api/toggle-watch _value))])))

(defn inspect-deref [value]
  (let [[active? set-active!] (use-watch value)
        value' #?(:clj  (react/use-atom value identity active?)
                  :cljs @value)]
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
