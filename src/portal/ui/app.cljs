(ns ^:no-doc portal.ui.app
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.api :as api]
            [portal.ui.commands :as commands]
            [portal.ui.connection-status :as status]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as opts]
            [portal.ui.react :as react]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.bin :as bin]
            [portal.ui.viewer.bytes :as bytes]
            [portal.ui.viewer.charts :as charts]
            [portal.ui.viewer.cljdoc :as cljdoc]
            [portal.ui.viewer.code :as code]
            [portal.ui.viewer.color :as color]
            [portal.ui.viewer.csv :as csv]
            [portal.ui.viewer.date-time :as date-time]
            [portal.ui.viewer.deref :as deref]
            [portal.ui.viewer.diff :as diff]
            [portal.ui.viewer.diff-text :as diff-text]
            [portal.ui.viewer.duration :as duration]
            [portal.ui.viewer.edn :as edn]
            [portal.ui.viewer.exception :as ex]
            [portal.ui.viewer.hiccup :as hiccup]
            [portal.ui.viewer.html :as html]
            [portal.ui.viewer.http :as http]
            [portal.ui.viewer.image :as image]
            [portal.ui.viewer.json :as json]
            [portal.ui.viewer.jwt :as jwt]
            [portal.ui.viewer.log :as log]
            [portal.ui.viewer.markdown :as md]
            [portal.ui.viewer.pprint :as pprint]
            [portal.ui.viewer.prepl :as prepl]
            [portal.ui.viewer.relative-time :as relative-time]
            [portal.ui.viewer.source-location :as source-location]
            [portal.ui.viewer.spec :as spec]
            [portal.ui.viewer.table :as table]
            [portal.ui.viewer.test-report :as test-report]
            [portal.ui.viewer.text :as text]
            [portal.ui.viewer.transit :as transit]
            [portal.ui.viewer.tree :as tree]
            [portal.ui.viewer.vega :as vega]
            [portal.ui.viewer.vega-lite :as vega-lite]
            [reagent.core :as r]))

(defn- select-viewer []
  (let [state            (state/use-state)
        theme            (theme/use-theme)
        selected-context (state/get-all-selected-context @state)
        viewer           (ins/get-viewer state (first selected-context))]
    [s/div
     {:title "Select a different viewer."
      :on-click #(commands/select-viewer state)
      :style
      {:display :flex
       :align-items :center
       :cursor :pointer
       :padding-left (:padding theme)
       :padding-right (:padding theme)
       :border-left [1 :solid (::c/border theme)]
       :border-right [1 :solid (::c/border theme)]}
      :style/hover {:background (::c/border theme)}}
     [s/div
      {:style {:opacity (if (seq selected-context) 1 0.5)}}
      [ins/with-readonly [ins/inspector (:name viewer)]]]]))

(defn- open-command-palette []
  (let [state (state/use-state)
        theme (theme/use-theme)]
    [s/div
     {:title    "Open command palette."
      :on-click #(commands/open-command-palette state)
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
        opts  (opts/use-options)
        path  (state/get-path @state)]
    [s/div
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
     [s/div
      {:title "Copy path."
       :on-click #(commands/copy-path state)
       :style/hover {:color (::c/tag theme)}
       :style
       {:color (::c/border theme)
        :cursor :pointer
        :box-sizing :border-box
        :padding (:padding theme)}}
      [icons/copy]]
     [select-viewer]
     [s/div
      {:style
       {:flex "1"
        :cursor :pointer
        :overflow :auto
        :display :flex
        :box-sizing :border-box
        :padding (:padding theme)
        :gap (:padding theme)}}
      [s/div {:style {:grid-row "1"}} "["]
      (map-indexed
       (fn [idx k]
         ^{:key idx}
         [s/div {:style {:grid-row "1"}} [ins/preview k]])
       path)
      [s/div {:style {:grid-row "1"}} "]"]]
     [open-command-palette]
     [s/div {:style
             {:display :flex
              :height "100%"
              :align-items :center
              :box-sizing :border-box
              :padding (* 0.5 (:padding theme))}}
      [log/icon (:runtime opts :portal)]]]))

(defn- use-runtime-info []
  (let [opts       (opts/use-options)
        theme      (theme/use-theme)
        header     (if (= :dev (:mode opts))
                     (::c/diff-add theme)
                     (::c/background2 theme))]
    (react/use-effect
     #js [header]
     (state/set-theme header)
     (state/notify-parent {:type :set-theme :color header}))
    (react/use-effect
     #js [opts]
     (when-let [{:keys [name platform version]} opts]
       (state/set-title!
        (str/join
         " - "
         [(:window-title opts name) platform version]))))))

(defn display-notification [notification]
  (let [state (state/use-state)
        theme (theme/use-theme)
        timeout (:timeout notification)]
    (react/use-effect
     :once
     (when timeout
       (js/setTimeout #(state/dispatch! state state/dismiss notification) timeout)))
    [s/div
     {:style
      {:display :flex
       :padding (:padding theme)
       :align-items :center
       :justify-content :space-between
       :background
       (case (:type notification)
         :error (::c/exception theme)
         :info (::c/boolean theme)
         (::c/background2 theme))
       :box-sizing :border-box
       :border-top [1 :solid (::c/border theme)]
       :animation [:fade-in-out (str (:timeout notification) "ms")]}}
     [s/div
      (when-let [icon (:icon notification)]
        (case icon
          :file-code [icons/file-code]
          :exclamation-triangle [icons/exclamation-triangle]
          nil))]
     [s/div
      {:style {:text-align :center :flex "1" :color (::c/text theme)}}
      [s/div (:title notification)]
      [s/div (:message notification)]]
     [s/div
      {:title "Dismiss notification."
       :style {:cursor :pointer}
       :on-click #(state/dispatch! state state/dismiss notification)}
      [icons/times]]]))

(defn inspect-footer []
  (let [state (state/use-state)]
    (use-runtime-info)
    (into
     [:<>
      [:style
       "@keyframes fade-in-out {"
       "0%, 100% { opacity: 0 }"
       "20%, 80% { opacity: 100 }"
       "}"]]
     (for [notification (::notifications @state)]
       [display-notification notification]))))

(defn- search-input []
  (let [ref      (react/use-ref nil)
        theme    (theme/use-theme)
        state    (state/use-state)
        context  (state/get-selected-context @state)
        location (state/get-location context)
        color    (if-let [depth (:depth context)]
                   (nth theme/order depth)
                   ::c/border)]
    (react/use-effect
     :always
     (swap! commands/search-refs conj ref)
     #(swap! commands/search-refs disj ref))

    [s/div
     {:style
      {:display :flex
       :position :relative
       :align-items :center}}
     [s/input
      {:ref ref
       :disabled  (nil? context)
       :on-change #(let [value (.-value (.-target %))]
                     (when context
                       (state/dispatch!
                        state
                        update
                        :search-text
                        (fn [filters]
                          (if (str/blank? value)
                            (dissoc filters location)
                            (assoc filters location value))))))
       :on-key-down (fn [e]
                      (when (= (.-key e) "Enter")
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
     (when (seq (:search-text @state))
       [s/div
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
           (commands/clear-filter state))}
        [icons/times-circle]])]))

(defn- button-hover [props child]
  (let [theme (theme/use-theme)]
    [s/div
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
    [s/div
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
     (let [disabled? (nil? @(r/cursor state [:portal/previous-state]))]
       [button-hover
        {:disabled disabled?
         :on-click #(state/dispatch! state state/history-back)}
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
     (let [disabled? (nil? @(r/cursor state [:portal/next-state]))]
       [button-hover
        {:disabled disabled?
         :on-click #(state/dispatch! state state/history-forward)}
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
      {:on-click #(state/dispatch! state state/clear)}
      [icons/ban
       {:title    "Clear all values from portal. - CTRL L"
        :size     "2x"
        :style    (merge
                   {:transform     "scale(0.75)"
                    :color         (::c/text theme)})}]]]))

(defn inspect-1 [value]
  (let [theme (theme/use-theme)
        state (state/use-state)
        ref   (react/use-ref)]
    (react/use-effect
     #js [(.-current ref)]
     (when-let [el (.-current ref)]
       (state/dispatch! state assoc :scroll-element el)))
    [s/div
     {:on-mouse-up
      (fn [e]
        (let [button (.-button e)]
          (cond
            (= 3 button)
            (commands/history-back state)
            (= 4 button)
            (commands/history-forward state))))
      :style
      {:height "100vh"
       :display :flex
       :flex-direction :column}}
     (when-not (:disable-shell? @state) [toolbar])
     [s/div
      {:style
       {:flex "1"
        :position :relative
        :min-width "100%"
        :box-sizing :border-box}}
      [s/div
       {:ref ref
        :on-click #(state/dispatch! state state/clear-selected)
        :style
        {:position :absolute
         :top 0
         :left 0
         :right 0
         :bottom 0
         :overflow :auto
         :box-sizing :border-box}}
       [s/div
        {:style {:min-width :fit-content}}
        [s/div
         {:style
          {:min-width :fit-content
           :box-sizing :border-box
           :padding (* 2 (:padding theme))}}
         [:> ins/error-boundary
          [select/with-position
           {:row 0 :column 0}
           [ins/inspector value]]]]]]]
     (when-not (:disable-shell? @state)
       [:<>
        [inspect-footer]
        [selected-context-view]])]))

(defn scrollbars []
  (let [theme (theme/use-theme)]
    (when-not (theme/is-vs-code?)
      [:style
       (str "*::-webkit-scrollbar { width: " (* 2 (:padding theme)) "px }"
            "*::-webkit-scrollbar-corner { opacity: 0 }"
            "*::-webkit-scrollbar-track  { opacity: 0 }"
            "*::-webkit-scrollbar-thumb  { background-color: " (::c/border theme) "; }"
            "*::-webkit-scrollbar-thumb  { border-radius: " (* 4 (:border-radius theme)) "px }")])))

(defn text-selection []
  (let [style "background: rgba(0,0,0,0.5)"]
    [:style
     (str "::selection { " style " }")
     (str "::-moz-selection { " style " }")]))

(defn styles []
  [:<>
   [vega/styles]
   [prepl/styles]
   [hiccup/styles]
   [text-selection]
   [code/stylesheet]])

(defn- container [children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:on-mouse-over
       (fn [_e]
         (reset! ins/hover? nil))
       :style
       {:-webkit-app-region (when-not (theme/is-vs-code?) :drag)
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
  (let [current-state @(state/use-state)]
    [:<>
     [commands/palette]
     (doall
      (map-indexed
       (fn [index state]
         ^{:key index}
         [s/div
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

(def viewers
  ^{:portal.viewer/default :portal.viewer/table
    :portal.viewer/table {:columns [:name :doc]}}
  [ex/viewer
   charts/line-chart
   charts/scatter-chart
   charts/histogram-chart
   log/viewer
   http/viewer
   image/viewer
   deref/viewer
   test-report/viewer
   prepl/viewer
   cljdoc/viewer
   color/viewer
   diff/deep-diff2
   ins/viewer
   ex/trace-viewer
   ex/sub-trace-viewer
   duration/nano
   duration/ms
   bytes/viewer
   pprint/viewer
   vega-lite/viewer
   vega/viewer
   bin/viewer
   table/viewer
   tree/viewer
   text/viewer
   code/viewer
   code/pr-str-viewer
   json/viewer
   jwt/viewer
   edn/viewer
   transit/viewer
   csv/viewer
   html/viewer
   diff/viewer
   diff-text/viewer
   md/viewer
   hiccup/viewer
   date-time/viewer
   relative-time/viewer
   source-location/viewer
   spec/viewer])

(reset! api/viewers viewers)

(defn root [& children]
  (let [opts  (opts/use-options)
        state state/state
        theme (or (:theme @state)
                  (:theme opts)
                  (::c/theme opts))]
    [state/with-state
     state
     [status/poller]
     [theme/with-theme
      theme
      [theme/with-background]
      [dnd/area
       [container children]]]]))

(defn app [value]
  [root
   [s/div
    {:style
     {:width "100%"
      :height "100%"
      :display :flex}}
    [inspect-1-history value]]])
