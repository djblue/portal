(ns portal.ui.app
  (:require ["react" :as react]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.commands :as commands]
            [portal.ui.connecton-status :as status]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as opts]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.bin :as bin]
            [portal.ui.viewer.charts :as charts]
            [portal.ui.viewer.csv :as csv]
            [portal.ui.viewer.date-time :as date-time]
            [portal.ui.viewer.diff :as diff]
            [portal.ui.viewer.edn :as edn]
            [portal.ui.viewer.exception :as ex]
            [portal.ui.viewer.hiccup :as hiccup]
            [portal.ui.viewer.html :as html]
            [portal.ui.viewer.image :as image]
            [portal.ui.viewer.json :as json]
            [portal.ui.viewer.markdown :as md]
            [portal.ui.viewer.relative-time :as relative-time]
            [portal.ui.viewer.table :as table]
            [portal.ui.viewer.text :as text]
            [portal.ui.viewer.transit :as transit]
            [portal.ui.viewer.tree :as tree]
            [portal.ui.viewer.vega :as vega]
            [portal.ui.viewer.vega-lite :as vega-lite]))

(defn- selected-context-view []
  (let [theme (theme/use-theme)
        state (state/use-state)
        path  (state/get-path @state)]
    [s/div
     {:style
      {:max-width "100vw"
       :display :flex
       :align-items :center
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
        :padding (:padding theme)
        :border-right [1 :solid (::c/border theme)]}}
      [icons/copy]]
     [s/div
      {:style
       {:cursor :pointer
        :overflow :auto
        :display :grid
        :box-sizing :border-box
        :padding (:padding theme)
        :grid-gap (:padding theme)}}
      [s/div {:style {:grid-row "1"}} "["]
      (map-indexed
       (fn [idx k]
         ^{:key idx}
         [s/div {:style {:grid-row "1"}} [ins/preview k]])
       path)
      [s/div {:style {:grid-row "1"}} "]"]]
     [s/div {:style {:flex "1"}}]]))

(defn- runtime-info []
  (let [opts       (opts/use-options)
        theme      (theme/use-theme)
        connected? (status/use-status)]
    (react/useEffect
     (fn []
       (when-let [{:keys [name platform version]} opts]
         (state/set-title!
          (str/join
           " - "
           [(:portal.launcher/window-title opts name) platform version]))))
     #js [opts])
    (when-not connected?
      [s/div
       {:style {:color (::c/exception theme)}}
       "ERROR: Disconnected from runtime!"])))

(defn inspect-1 [value]
  (let [theme      (theme/use-theme)
        state      (state/use-state)
        selected-context (state/get-selected-context @state)
        viewer           (ins/get-viewer state selected-context)
        compatible-viewers (ins/get-compatible-viewers
                            @ins/viewers
                            (:value selected-context))]
    [s/div
     {:style
      {:height "calc(100vh - 64px)"
       :display :flex
       :flex-direction :column}}
     [s/div
      {:style
       {:flex "1"
        :position :relative
        :min-width "100%"
        :box-sizing :border-box}}
      [s/div
       {:ref commands/scroll-div
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
     [s/div
      {:style
       {:display :flex
        :min-height 63
        :align-items :center
        :justify-content :space-between
        :background (::c/background2 theme)
        :box-sizing :border-box
        :padding (:padding theme)
        :border-top [1 :solid (::c/border theme)]}}
      (when-not (empty? compatible-viewers)
        [s/select
         {:title "Select a different viewer."
          :value (pr-str (:name viewer))
          :on-change
          (fn [e]
            (ins/set-viewer!
             state
             selected-context
             (keyword (.substr (.. e -target -value) 1))))
          :style
          {:background (::c/background theme)
           :padding (:padding theme)
           :box-sizing :border-box
           :font-family (:font-family theme)
           :font-size (:font-size theme)
           :color (::c/text theme)
           :border-radius (:border-radius theme)
           :border [1 :solid (::c/border theme)]}}
         (for [{:keys [name]} compatible-viewers]
           ^{:key name}
           [s/option {:value (pr-str name)} (pr-str name)])])
      [runtime-info]
      [s/button
       {:title    "Open command palette."
        :on-click #(commands/open-command-palette state)
        :style
        {:min-width 60
         :font-family (:font-family theme)
         :background (::c/background theme)
         :border-radius (:border-radius theme)
         :border [1 :solid (::c/border theme)]
         :box-sizing :border-box
         :padding-top (:padding theme)
         :padding-bottom (:padding theme)
         :padding-left (:padding theme)
         :padding-right (* 1 (:padding theme))
         :color (::c/tag theme)
         :font-size (:font-size theme)
         :font-weight :bold
         :cursor :pointer}}
       [icons/terminal]]]
     [selected-context-view]]))

(defn search-input []
  (let [theme    (theme/use-theme)
        state    (state/use-state)
        context  (state/get-selected-context @state)
        location (state/get-location context)]
    [s/input
     {:ref commands/filter-input
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
                       (ins/focus-selected)))
      :value (get-in @state [:search-text location] "")
      :placeholder (if-not context
                     "Select a value to enable filtering"
                     "Type here to begin filtering")
      :style
      {:background (::c/background theme)
       :padding (:padding theme)
       :box-sizing :border-box
       :font-family (:font-family theme)
       :font-size (:font-size theme)
       :color (::c/boolean theme)
       :border [1 :solid (::c/border theme)]
       :border-radius (:border-radius theme)}
      :style/placeholder
      {:color (if-not context (::c/border theme) (::c/text theme))}}]))

(defn button-styles []
  (let [theme (theme/use-theme)]
    {:background (::c/text theme)
     :color (::c/background theme)
     :border :none
     :font-size (:font-size theme)
     :font-family "Arial"
     :box-sizing :border-box
     :padding-left (inc (:padding theme))
     :padding-right (inc (:padding theme))
     :padding-top (inc (:padding theme))
     :padding-bottom (inc (:padding theme))
     :border-radius (:border-radius theme)
     :cursor :pointer}))

(defn toolbar []
  (let [theme (theme/use-theme)
        state (state/use-state)]
    [s/div
     {:style
      {:display :grid
       :grid-template-columns "auto auto 1fr auto"
       :padding-left (* 2 (:padding theme))
       :padding-right (* 2 (:padding theme))
       :box-sizing :border-box
       :grid-gap (* 2 (:padding theme))
       :height 63
       :background (::c/background2 theme)
       :align-items :center
       :justify-content :center
       :border-top [1 :solid (::c/border theme)]
       :border-bottom [1 :solid (::c/border theme)]}}
     (let [disabled? (nil? (:portal/previous-state @state))]
       [s/button
        {:disabled disabled?
         :title    "Go back in portal history."
         :on-click #(state/dispatch! state state/history-back)
         :style    (merge
                    (button-styles)
                    (when disabled?
                      {:opacity 0.45
                       :cursor  :default}))}
        [icons/arrow-left]])
     (let [disabled? (nil? (:portal/next-state @state))]
       [s/button
        {:disabled disabled?
         :title    "Go forward in portal history."
         :on-click #(state/dispatch! state state/history-forward)
         :style    (merge
                    (button-styles)
                    (when disabled?
                      {:opacity 0.45
                       :cursor  :default}))}
        [icons/arrow-right]])
     [search-input]
     [s/button
      {:title    "Clear all values from portal."
       :on-click #(state/dispatch! state state/clear)
       :style    (merge
                  (button-styles)
                  {:padding-left (* 2 (:padding theme))
                   :padding-right (* 2 (:padding theme))})}
      "clear"]]))

(defn scrollbars []
  (let [thumb "rgba(0,0,0,0.3)"]
    [:style
     (str "* { scrollbar-color: " thumb " rgba(0,0,0,0); } "
          "*::-webkit-scrollbar { width: 10px; height: 10px; }"
          "*::-webkit-scrollbar-corner { opacity: 0 }"
          "*::-webkit-scrollbar-track  { opacity: 0 }"
          "*::-webkit-scrollbar-thumb  { background-color: " thumb "; }"
          "*::-webkit-scrollbar-thumb  { border-radius: 10px; }")]))

(defn text-selection []
  (let [style "background: rgba(0,0,0,0.5)"]
    [:style
     (str "::selection { " style " }")
     (str "::-moz-selection { " style " }")]))

(defn- container [children]
  (let [theme (theme/use-theme)]
    (into
     [s/div
      {:style
       {:display :flex
        :flex-direction :column
        :background (::c/background theme)
        :color (::c/text theme)
        :font-family (:font-family theme)
        :font-size (:font-size theme)
        :height "100vh"
        :width "100vw"}}
      [scrollbars]
      [text-selection]]
     children)))

(defn- use-viewer-commands []
  (let [state              (state/use-state)
        selected-context   (state/get-selected-context @state)
        compatible-viewers (ins/get-compatible-viewers @ins/viewers (:value selected-context))]
    (map #(-> %
              (dissoc :predicate)
              (assoc  :run
                      (fn [] (ins/set-viewer! state selected-context (:name %)))))
         compatible-viewers)))

(defn- inspect-1-history [default-value]
  (let [current-state @(state/use-state)
        commands      (use-viewer-commands)]
    [:<>
     [commands/palette {:commands commands}]
     (doall
      (map-indexed
       (fn [index state]
         ^{:key index}
         [s/div
          {:style
           {:flex 1
            :display
            (if (= state current-state)
              :block
              :none)}}
          [select/with-position
           {:row 0 :column index}
           [inspect-1 (state/get-value state default-value)]]])
       (state/get-history current-state)))]))

(def viewers
  [ex/viewer
   vega-lite/viewer
   vega/viewer
   charts/line-chart
   charts/scatter-chart
   charts/histogram-chart
   image/viewer
   ins/viewer
   bin/viewer
   table/viewer
   tree/viewer
   text/viewer
   json/viewer
   edn/viewer
   transit/viewer
   csv/viewer
   html/viewer
   diff/viewer
   md/viewer
   hiccup/viewer
   date-time/viewer
   relative-time/viewer])

(reset! ins/viewers viewers)

(defn root [& children]
  (let [opts  (opts/use-options)
        state state/state
        theme (or (::c/theme @state)
                  (::c/theme opts)
                  ::c/nord)]
    (react/useEffect
     (fn []
       (state/dispatch! state state/set-theme! theme))
     #js [theme])
    [state/with-state
     state
     [theme/with-theme
      theme
      [container children]]]))

(defn app [value]
  [root
   [toolbar]
   [s/div {:style {:height "calc(100vh - 64px)" :width "100vw"}}
    [s/div
     {:style
      {:width "100%"
       :height "100%"
       :display :flex}}
     [inspect-1-history value]]]])
