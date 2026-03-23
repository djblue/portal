(ns portal.ssr.ui.app
  (:require
   [clojure.string :as str]
   [portal.colors :as c]
   [portal.ssr.ui.commands :as commands]
   [portal.ssr.ui.icons :as icons]
   [portal.ssr.ui.inspector :as ins]
   [portal.ssr.ui.react :as react]
   [portal.ssr.ui.select :as select]
   [portal.ssr.ui.state :as state]
   [portal.ssr.ui.styled :as d]
   [portal.ssr.ui.theme :as theme]
   [portal.ssr.ui.viewer.date-time :as date-time]
   [portal.ssr.ui.viewer.deref :as deref]
   [portal.ssr.ui.viewer.log :as log]
   [portal.ssr.ui.viewer.source-location :as source-location]))

(defn- search-input []
  (let [;ref      (react/use-ref nil)
        theme    (theme/use-theme)
        state    (state/use-state)
        context  (react/use-atom state state/get-selected-context)
        location (state/get-location context)
        color    (if-let [depth (:depth context)]
                   (nth theme/order depth)
                   ::c/border)]
    ;; (react/use-effect
    ;;  :always
    ;;  (swap! commands/search-refs conj ref)
    ;;  #(swap! commands/search-refs disj ref))

    [d/div
     {:style
      {:display :flex
       :position :relative
       :align-items :center}}
     [d/input
      {;:ref ref
       :disabled  (nil? context)
       :on-change (fn [e]
                    (let [value (get-in e [:target :value])]
                      (when context
                        (state/dispatch!
                         state
                         update
                         :search-text
                         (fn [filters]
                           (if (str/blank? value)
                             (dissoc filters location)
                             (assoc filters location value)))))))
       #_#_:on-key-down (fn [e]
                          (tap> e)
                          #_(when (= (.-key e) "Enter")
                              (.blur (.-current ref))))
       :value (get-in @state [:search-text location] "")
       :placeholder (if-not context
                      "Select a value to enable filtering"
                      "Type here to begin filtering")
       :style
       {:flex "1"
        :background (::c/background theme)
        :padding (* 0.75 (:padding theme))
        :box-sizing :border-box
        :font-family (:font-family theme)
        :font-size (:font-size theme)
        :color (get theme color)
        :border [1 :solid (::c/border theme)]
        :border-radius (:border-radius theme)}
       :style/placeholder
       {:color (if-not context (::c/border theme) (::c/text theme))}}]
     (when (seq (react/use-atom state :search-text))
       [d/div
        {:title "Clear all filters."
         :style
         {:cursor :pointer
          :position :absolute
          :right (:padding theme)
          :color (::c/border theme)}
         :style/hover
         {:color (::c/exception theme)}
         :on-click
         (fn [_]
           (state/dispatch! state state/clear-search))}
        [icons/times-circle]])]))

(defn- button-hover [props child]
  (let [theme (theme/use-theme)]
    [d/div
     (merge
      {:style {:display         :flex
               :width           "2rem"
               :height          "2rem"
               :border-radius   "100%"
               :align-items     :center
               :justify-content :center
               :cursor          :pointer
               :border          [1 :solid "rgba(0,0,0,0)"]}}
      props
      (when-not (:disabled props)
        {:style/hover
         {:background (::c/background theme)
          :border     [1 :solid (::c/border theme)]}}))
     child]))

(defn- toolbar []
  (let [theme (theme/use-theme)
        state (state/use-state)]
    [d/div
     {:style
      {:display :grid
       :grid-template-columns "auto auto 1fr auto"
       :box-sizing :border-box
       :padding-top (* 0.5 (:padding theme))
       :padding-bottom (* 0.5 (:padding theme))
       :padding-right (:padding theme)
       :padding-left (:padding theme)
       :grid-gap (* 0.5 (:padding theme))
       :background (::c/background2 theme)
       :align-items :center
       :justify-content :center
       :border-top [1 :solid (::c/border theme)]
       :border-bottom [1 :solid (::c/border theme)]}}
     (let [disabled? (react/use-atom state #(nil? (get-in % [:portal/previous-state])))]
       [button-hover
        {:disabled disabled?
         :on-click (fn [_] (state/dispatch! state state/history-back))}
        [icons/arrow-left
         {:disabled disabled?
          :title    "Go back in portal history."
          :size     "2x"
          :style    (merge
                     {:transform     "scale(0.75)"
                      :color         (::c/text theme)}
                     (when disabled?
                       {:opacity 0.45
                        :cursor  :default}))}]])
     (let [disabled? (react/use-atom state #(nil? (get-in % [:portal/next-state])))]
       [button-hover
        {:disabled disabled?
         :on-click (fn [_] (state/dispatch! state state/history-forward))}
        [icons/arrow-right
         {:disabled disabled?
          :title    "Go forward in portal history."
          :size     "2x"
          :style    (merge
                     {:transform     "scale(0.75)"
                      :color         (::c/text theme)}
                     (when disabled?
                       {:opacity 0.45
                        :cursor  :default}))}]])
     [search-input]
     [button-hover
      {:on-click (fn [_] (state/dispatch! state state/clear))}
      [icons/ban
       {:title    "Clear all values from portal. - CTRL L"
        :size     "2x"
        :style    (merge
                   {:transform     "scale(0.75)"
                    :color         (::c/text theme)})}]]]))

(defn- select-viewer []
  (let [state            (state/use-state)
        theme            (theme/use-theme)
        selected-context (react/use-atom state state/get-all-selected-context)
        viewer           (ins/get-viewer state (first selected-context))]
    [d/div
     {:title "Select a different viewer."
      :on-click (fn [_] (commands/select-viewer state))
      :style
      {:display :flex
       :align-items :center
       :cursor :pointer
       :padding-left (:padding theme)
       :padding-right (:padding theme)
       :border-left [1 :solid (::c/border theme)]
       :border-right [1 :solid (::c/border theme)]}
      :style/hover {:background (::c/border theme)}}
     [d/div
      {:style {:opacity (if (seq selected-context) 1 0.5)}}
      [ins/with-readonly [ins/inspector (:name viewer)]]]]))

(defn- open-command-palette []
  (let [state (state/use-state)
        theme (theme/use-theme)]
    [d/div
     {:title    "Open command palette."
      :on-click (fn [_] (commands/open-command-palette state))
      :style
      {:padding-left (* 2 (:padding theme))
       :padding-right (* 2 (:padding theme))
       :color (::c/tag theme)
       :display :flex
       :align-items :center
       :cursor :pointer
       :border-left [1 :solid (::c/border theme)]
       :border-right [1 :solid (::c/border theme)]}
      :style/hover {:background (::c/border theme)}}
     [icons/terminal]]))

(defn- selected-context-view []
  (let [theme (theme/use-theme)
        state (state/use-state)
        ;; opts  (opts/use-options)
        path  (react/use-atom state state/get-path)]
    [d/div
     {:style
      {:max-width "100vw"
       :padding-left (:padding theme)
       :padding-right (:padding theme)
       :gap (:padding theme)
       :display :flex
       :align-items :stretch
       :justify-content :space-between
       :background (::c/background2 theme)
       :color (::c/text theme)
       :border-top [1 :solid (::c/border theme)]}}
     [d/div
      {:title "Copy path."
       :on-click (fn [_] (commands/copy-path state))
       :style/hover {:color (::c/tag theme)}
       :style
       {:color (::c/border theme)
        :cursor :pointer
        :box-sizing :border-box
        :padding (:padding theme)}}
      [icons/copy]]
     [select-viewer]
     [d/div]
     [d/div
      {:style
       {:flex "1"
        :cursor :pointer
        :overflow :auto
        :display :flex
        :box-sizing :border-box
        :padding (:padding theme)
        :gap (:padding theme)}}
      [d/div {:style {:grid-row "1"}} "["]
      (map-indexed
       (fn [idx k]
         ^{:key idx}
         [d/div {:style {:grid-row "1"}} [ins/preview k]])
       path)
      [d/div {:style {:grid-row "1"}} "]"]]
     [open-command-palette]
     [d/div {:style
             {:display :flex
              :height "100%"
              :align-items :center
              :box-sizing :border-box
              :padding (* 0.5 (:padding theme))}}
      #_[log/icon (:runtime opts :portal)]]]))

(defn inspect-1 [value]
  (let [theme (theme/use-theme)
        state (state/use-state)
        ;; ref   (react/use-ref)
        disable-shell? (react/use-atom state :disable-shell?)]
    ;; (react/use-effect
    ;;  #js [(.-current ref)]
    ;;  (when-let [el (.-current ref)]
    ;;    (state/dispatch! state assoc :scroll-element el)))
    [d/div
     {:on-mouse-up
      (fn [_]
        #_(let [button (.-button e)]
            (cond
              (= 3 button)
              (commands/history-back state)
              (= 4 button)
              (commands/history-forward state))))
      :style
      {:height "100vh"
       :display :flex
       :flex-direction :column}}
     (when-not disable-shell? [toolbar])
     [d/div
      {:style
       {:flex "1"
        :position :relative
        :min-width "100%"
        :box-sizing :border-box}}
      [d/div
       {;:ref ref
        :on-click (fn [_] (state/dispatch! state state/clear-selected))
        :style
        {:position :absolute
         :top 0
         :left 0
         :right 0
         :bottom 0
         :overflow :auto
         :box-sizing :border-box}}
       [d/div
        {:style {:min-width :fit-content}}
        [d/div
         {:style
          {:min-width :fit-content
           :box-sizing :border-box
           :padding (* 2 (:padding theme))}}
         [ins/inspector value]]]]]
     (when-not disable-shell?
       [:<>
        #_[inspect-footer]
        [selected-context-view]])]))

(defn scrollbars []
  (let [theme (theme/use-theme)
        size (* 1.5 (:padding theme))]
    [:style
     (str "*::-webkit-scrollbar { width: " size "px; height: " size "px; }"
          "*::-webkit-scrollbar-corner { opacity: 0 }"
          "*::-webkit-scrollbar-track  { opacity: 0 }"
          "*::-webkit-scrollbar-thumb  { background-color: " (::c/border theme) "; }"
          "*::-webkit-scrollbar-thumb  { border-radius: " (* 4 (:border-radius theme)) "px }")]))

(defn text-selection []
  (let [style "background: rgba(0,0,0,0.5)"]
    [:style
     (str "::selection { " style " }")
     (str "::-moz-selection { " style " }")]))

(defn styles []
  [:<>
   #_[vega/styles]
   #_[prepl/styles]
   #_[hiccup/styles]
   [text-selection]
   #_[code/stylesheet]])

(defn- container [children]
  (let [theme (theme/use-theme)]
    (into
     [d/div
      {#_#_:on-mouse-over
         (fn [_e]
           (reset! ins/hover? nil))
       :style
       {;:-webkit-app-region (when-not (theme/is-vs-code?) :drag)
        :display :flex
        :flex-direction :column
        :background (::c/background theme)
        :color (::c/text theme)
        :font-family (:font-family theme)
        :font-size (:font-size theme)
        :height "100vh"
        :width "100vw"}}
      [styles]
      [scrollbars]]
     children)))

(defn- inspect-1-history [default-value]
  (let [current-state (react/use-atom (state/use-state))]
    [:<>
     [commands/palette]
     (doall
      (map-indexed
       (fn [index state]
         ^{:key index}
         [d/div
          {:style
           {:flex "1"
            :display
            (if (= state current-state)
              :block
              :none)}}
          [select/with-position
           {:row 0 :column index}
           [inspect-1 (state/get-value state default-value)]]])
       (state/get-history current-state)))]))

(defn root [{:keys [state]} & children]
  (let [theme (or (react/use-atom state :theme) ::c/nord)]
    [state/with-state
     state
     [theme/with-theme
      theme
      [icons/mount-stylesheet]
      [container children]]]))

(defn app [session]
  (let [value (-> session :options :value)]
    [root
     session
     [d/div
      {:style
       {:width "100%"
        :height "100%"
        :display :flex}}
      [inspect-1-history value]]]))

(reset! ins/viewers
        [deref/viewer
         log/viewer
         ins/viewer
         source-location/viewer
         date-time/viewer])