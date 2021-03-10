(ns portal.ui.app
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.commands :as commands]
            [portal.ui.inspector :as ins]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.charts :as charts]
            [portal.ui.viewer.csv :as csv]
            [portal.ui.viewer.diff :as diff]
            [portal.ui.viewer.edn :as edn]
            [portal.ui.viewer.exception :as ex]
            [portal.ui.viewer.hiccup :as hiccup]
            [portal.ui.viewer.html :as html]
            [portal.ui.viewer.image :as image]
            [portal.ui.viewer.json :as json]
            [portal.ui.viewer.markdown :as md]
            [portal.ui.viewer.table :as table]
            [portal.ui.viewer.text :as text]
            [portal.ui.viewer.transit :as transit]
            [portal.ui.viewer.tree :as tree]
            [portal.ui.viewer.vega-lite :as vega-lite]
            [portal.ui.viewer.vega :as vega]
            [reagent.core :as r]))

(defn filter-data [search-text value]
  (let [filter-data (partial filter-data search-text)]
    (if (str/blank? search-text)
      value
        ;:diff
      (case (ins/get-value-type value)
        :map
        (let [new-value (->>
                         (for [[k v] value]
                           (let [filter-k (filter-data k)
                                 filter-v (filter-data v)]
                             (if (= ::not-found filter-k filter-v)
                               ::not-found
                               [(if (= filter-k ::not-found)
                                  k
                                  filter-k)
                                (if (= filter-v ::not-found)
                                  v
                                  filter-v)])))
                         (remove #{::not-found})
                         (into {}))]
          (if (empty? new-value)
            ::not-found
            (with-meta new-value (meta value))))

        :set
        (let [new-value (->> value
                             (map filter-data)
                             (remove #{::not-found})
                             (into #{}))]
          (if (empty? new-value)
            ::not-found
            (with-meta new-value (meta value))))

        :vector
        (let [new-value  (->> value
                              (map filter-data)
                              (remove #{::not-found})
                              (into []))]
          (if (empty? new-value)
            ::not-found
            (with-meta new-value (meta value))))

        (:list
         :coll)
        (let [new-value (->> value
                             (take 1000)
                             (map filter-data)
                             (remove #{::not-found}))]
          (if (empty? new-value)
            ::not-found
            (with-meta new-value (meta value))))

        (:boolean
         :symbol
         :number
         :string
         :keyword
         :var
         :exception
         :object
         :uuid
         :uri
         :date
         :tagged)
        (if (str/includes? (pr-str value) search-text)
          value
          ::not-found)

        ::not-found))))

(def viewers
  [ex/viewer
   vega-lite/viewer
   vega/viewer
   charts/line-chart
   charts/scatter-chart
   charts/histogram-chart
   image/viewer
   ins/viewer
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
   hiccup/viewer])

(def viewers-by-name
  (into {} (map (juxt :name identity) viewers)))

(defn use-viewer [value]
  (let [state              (state/use-state)
        selected-viewer    (::selected-viewer @state)
        default-viewer     (get viewers-by-name (:portal.viewer/default (meta value)))
        viewers            (cons default-viewer (remove #(= default-viewer %) viewers))
        compatible-viewers (filter #(when-let [pred (:predicate %)] (pred value)) viewers)]
    {:compatible-viewers compatible-viewers
     :viewer
     (or
      (some #(when (= (:name %) selected-viewer) %)
            compatible-viewers)
      (first compatible-viewers))
     :set-viewer!
     (fn [viewer]
       (state/dispatch! state assoc ::selected-viewer viewer))}))

(def error-boundary
  (r/create-class
   {:display-name "ErrorBoundary"
    :constructor
    (fn [this _props]
      (set! (.-state this) #js {:error nil}))
    :component-did-catch
    (fn [_this _e _info])
    :get-derived-state-from-error
    (fn [error] #js {:error error})
    :render
    (fn [this]
      (if-let [error (.. this -state -error)]
        (r/as-element
         [:div [:pre [:code (pr-str error)]]])
        (.. this -props -children)))}))

(defn inspect-1 [value]
  (let [theme   (theme/use-theme)
        value   (filter-data (:search-text @(state/use-state)) value)
        {:keys [compatible-viewers viewer set-viewer!]} (use-viewer value)
        component (:component viewer)]
    [:<>
     [s/div
      {:style
       {:position :relative
        :min-height "calc(100% - 64px)"
        :max-height "calc(100% - 64px)"
        :min-width "100%"
        :box-sizing :border-box}}
      [s/div
       {:style
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
           :padding (* 2 (:spacing/padding theme))}}
         [:> error-boundary [component value]]]]]]
     [s/div
      {:style
       {:display :flex
        :min-height 63
        :align-items :center
        :justify-content :space-between
        :background (::c/background2 theme)
        :border-top [1 :solid (::c/border theme)]}}
      (if (empty? compatible-viewers)
        [s/div]
        [:select
         {:value (pr-str (:name viewer))
          :on-change #(set-viewer!
                       (keyword (.substr (.. % -target -value) 1)))
          :style
          {:background (::c/background theme)
           :margin (:spacing/padding theme)
           :padding (:spacing/padding theme)
           :box-sizing :border-box
           :font-size (:font-size theme)
           :color (::c/text theme)
           :border [1 :solid (::c/border theme)]}}
         (for [{:keys [name]} compatible-viewers]
           [:option {:key name :value (pr-str name)} (pr-str name)])])]]))

(defn search-input []
  (let [theme (theme/use-theme)
        state (state/use-state)]
    [s/input
     {:on-change #(state/dispatch!
                   state
                   assoc
                   :search-text
                   (.-value (.-target %)))
      :value (or (:search-text @state) "")
      :placeholder "Type to filter..."
      :style
      {:background (::c/background theme)
       :padding (:spacing/padding theme)
       :box-sizing :border-box
       :font-size (:font-size theme)
       :color (::c/text theme)
       :border [1 :solid (::c/border theme)]}}]))

(defn button-styles []
  (let [theme (theme/use-theme)]
    {:background (::c/text theme)
     :color (::c/background theme)
     :border :none
     :font-size (:font-size theme)
     :font-family "Arial"
     :box-sizing :border-box
     :padding-left (inc (:spacing/padding theme))
     :padding-right (inc (:spacing/padding theme))
     :padding-top (inc (:spacing/padding theme))
     :padding-bottom (inc (:spacing/padding theme))
     :border-radius (:border-radius theme)
     :cursor :pointer}))

(defn toolbar []
  (let [theme (theme/use-theme)
        state (state/use-state)]
    [s/div
     {:style
      {:display :grid
       :grid-template-columns "auto auto 1fr auto"
       :padding-left (* 2 (:spacing/padding theme))
       :padding-right (* 2 (:spacing/padding theme))
       :box-sizing :border-box
       :grid-gap (* 2 (:spacing/padding theme))
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
        "◄"])
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
        "►"])
     [search-input]
     [s/button
      {:title    "Clear all values from portal."
       :on-click #(state/dispatch! state state/clear)
       :style    (merge
                  (button-styles)
                  {:padding-left (* 2 (:spacing/padding theme))
                   :padding-right (* 2 (:spacing/padding theme))})}
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
        :font-family (:font/family theme)
        :font-size (:font-size theme)
        :height "100vh"
        :width "100vw"}}
      [scrollbars]
      [text-selection]]
     children)))

(defn- viewer-commands [value]
  (let [{:keys [compatible-viewers set-viewer!]} (use-viewer value)]
    (map #(-> %
              (dissoc :predicate)
              (assoc  :run (fn [] (set-viewer! (:name %)))))
         compatible-viewers)))

(defn- inspect-1-history []
  (let [current-state @(state/use-state)]
    [:<>
     [commands/palette
      {:commands (viewer-commands (state/get-selected-value current-state))}]
     [s/div {:style {:flex 1}}
      [inspect-1 (state/get-value current-state)]]]))

(defn root [& children]
  [state/with-state
   state/state
   [theme/with-theme
    (get state/state ::c/theme ::c/nord)
    [container children]]])

(defn app []
  [root
   [toolbar]
   [s/div {:style {:height "calc(100vh - 64px)" :width "100vw"}}
    [s/div
     {:style
      {:width "100%"
       :height "100%"
       :display :flex}}
     [inspect-1-history]]]])
