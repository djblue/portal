(ns portal.ui.app
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.commands :as commands]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.inspector :as ins :refer [inspector]]
            [portal.ui.state :refer [state tap-state]]
            [portal.ui.styled :as s]
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
            [reagent.core :as r]))

(defn filter-data [settings value]
  (let [search-text (:search-text settings)
        filter-data (partial filter-data settings)]
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

(defonce show-meta? (r/atom false))

(defn inspect-metadata [settings value]
  (when-let [m (meta value)]
    [s/div
     {:style
      {:border-bottom (str "1px solid " (::c/border settings))}}
     [s/div
      {:on-click #(swap! show-meta? not)
       :style/hover {:color (::c/tag settings)}
       :style {:cursor :pointer
               :user-select :none
               :color (::c/namespace settings)
               :padding-top (* 1 (:spacing/padding settings))
               :padding-bottom (* 1 (:spacing/padding settings))
               :padding-left (* 2 (:spacing/padding settings))
               :padding-right (* 2 (:spacing/padding settings))
               :font-size "16pt"
               :font-weight 100
               :display :flex
               :justify-content :space-between
               :background (::c/background2 settings)
               :font-family "sans-serif"}}
      "metadata"
      [s/div
       {:title "toggle metadata"
        :style {:font-weight :bold}}
       (if @show-meta?  "—" "+")]]
     (when @show-meta?
       [s/div
        {:style
         {:border-top (str "1px solid " (::c/border settings))
          :box-sizing :border-box
          :padding (* 2 (:spacing/padding settings))}}
        [inspector (assoc settings :coll value :depth 0) m]])]))

(def viewers
  [ex/viewer
   image/viewer
   {:name :portal.viewer/map  :predicate map?  :component ins/inspect-map}
   {:name :portal.viewer/coll :predicate coll? :component ins/inspect-coll}
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

(defn use-viewer [settings value]
  (let [set-settings!      (:set-settings! settings)
        selected-viewer    (:selected-viewer settings)
        default-viewer     (get viewers-by-name (:portal.viewer/default (meta value)))
        viewers            (cons default-viewer viewers)
        compatible-viewers (filter #(when-let [pred (:predicate %)] (pred value)) viewers)]
    {:compatible-viewers compatible-viewers
     :viewer 
     (or
       (some #(when (= (:name %) selected-viewer) %)
             compatible-viewers)
       (first compatible-viewers))
     :set-viewer!
     (fn [viewer]
       (set-settings! {:selected-viewer viewer}))}))

(defn get-datafy [settings]
  (fn [value]
    (try
      ((get-in (use-viewer settings (:portal/value settings)) [:viewer :datafy] identity)
       (filter-data settings value))
      (catch js/Error _e value))))

(defn- error-boundary []
  (let [error         (r/atom nil)
        last-children (atom nil)]
    (r/create-class
     {:display-name "ErrorBoundary"
      :component-did-catch
      (fn [_e info]
        (reset! error [@last-children info]))
      :reagent-render
      (fn [& children]
        (when-not (= children (first @error))
          (reset! last-children children)
          (reset! error nil))
        (if (nil? @error)
          (into [:<>] children)
          (let [[_ info] @error]
            [:pre [:code (pr-str info)]])))})))

(defn inspect-1 [settings value]
  (let [value (filter-data settings value)
        {:keys [compatible-viewers viewer set-viewer!]} (use-viewer settings value)
        component (or (:component viewer) inspector)
        settings  (assoc settings
                         :portal/rainbow
                         (cycle ((juxt ::c/exception ::c/keyword ::c/string
                                       ::c/tag ::c/number ::c/uri) settings)))
        commands  (map #(-> %
                            (dissoc :predicate)
                            (assoc  :run (fn [] (set-viewer! (:name %)))))
                       compatible-viewers)]
    [s/div
     {:style {:flex 1}}
     [commands/palette
      (assoc settings
             :datafy (get-datafy settings)
             :commands commands)]
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
        [inspect-metadata settings value]
        [s/div
         {:style
          {:box-sizing :border-box
           :padding (* 2 (:spacing/padding settings))}}
         [error-boundary
          [inspector (assoc settings :component component) value]]]]]]
     [s/div
      {:style
       {:display :flex
        :min-height 63
        :align-items :center
        :justify-content :space-between
        :background (::c/background2 settings)
        :border-top (str "1px solid " (::c/border settings))}}
      (if (empty? compatible-viewers)
        [s/div]
        [:select
         {:value (pr-str (:name viewer))
          :on-change #(set-viewer!
                       (keyword (.substr (.. % -target -value) 1)))
          :style
          {:background (::c/background settings)
           :margin (:spacing/padding settings)
           :padding (:spacing/padding settings)
           :box-sizing :border
           :font-size (:font-size settings)
           :color (::c/text settings)
           :border (str "1px solid " (::c/border settings))}}
         (for [{:keys [name]} compatible-viewers]
           [:option {:key name :value (pr-str name)} (pr-str name)])])
      [s/div
       {:style {:padding (:spacing/padding settings)}}
       [ins/preview settings value]]]]))

(defn search-input [settings]
  [s/input
   {:on-change #((:set-settings! settings)
                 {:search-text (.-value (.-target %))})
    :value (:search-text settings)
    :placeholder "Type to filter..."
    :style
    {:background (::c/background settings)
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :font-size (:font-size settings)
     :color (::c/text settings)
     :border (str "1px solid " (::c/border settings))}}])

(defn button-styles [settings]
  {:background (::c/text settings)
   :color (::c/background settings)
   :border :none
   :font-size (:font-size settings)
   :font-family "Arial"
   :box-sizing :border-box
   :padding-left (inc (:spacing/padding settings))
   :padding-right (inc (:spacing/padding settings))
   :padding-top (inc (:spacing/padding settings))
   :padding-bottom (inc (:spacing/padding settings))
   :border-radius (:border-radius settings)
   :cursor :pointer})

(defn toolbar [settings]
  [s/div
   {:style
    {:display :grid
     :grid-template-columns "auto auto 1fr auto"
     :padding-left (* 2 (:spacing/padding settings))
     :padding-right (* 2 (:spacing/padding settings))
     :box-sizing :border-box
     :grid-gap (* 2 (:spacing/padding settings))
     :height 63
     :background (::c/background2 settings)
     :align-items :center
     :justify-content :center
     :border-top (str "1px solid " (::c/border settings))
     :border-bottom (str "1px solid " (::c/border settings))}}
   (let [disabled? (nil? (:portal/previous-state settings))]
     [s/button
      {:disabled disabled?
       :title    "Go back in portal history."
       :on-click (:portal/on-back settings)
       :style    (merge
                  (button-styles settings)
                  (when disabled?
                    {:opacity 0.45
                     :cursor  :default}))}
      "◄"])
   (let [disabled? (nil? (:portal/next-state settings))]
     [s/button
      {:disabled disabled?
       :title    "Go forward in portal history."
       :on-click (:portal/on-forward settings)
       :style    (merge
                  (button-styles settings)
                  (when disabled?
                    {:opacity 0.45
                     :cursor  :default}))}
      "►"])
   [search-input settings]
   (let [disabled? (not (contains? settings :portal/value))]
     [s/button
      {:disabled disabled?
       :title    "Clear all values from portal."
       :on-click (:portal/on-clear settings)
       :style    (merge
                  (button-styles settings)
                  (when disabled?
                    {:opacity 0.45
                     :cursor  :default})
                  {:padding-left (* 2 (:spacing/padding settings))
                   :padding-right (* 2 (:spacing/padding settings))})}
      "clear"])])

(defn scrollbars []
  (let [thumb "rgba(0,0,0,0.3)"]
    [:style
     (str "* { scrollbar-color: " thumb " rgba(0,0,0,0); } "
          "*::-webkit-scrollbar { width: 10px; height: 10px; }"
          "*::-webkit-scrollbar-corner { opacity: 0 }"
          "*::-webkit-scrollbar-track  { opacity: 0 }"
          "*::-webkit-scrollbar-thumb  { background-color: " thumb "; }"
          "*::-webkit-scrollbar-thumb  { border-radius: 10px; }")]))

(defn app []
  (let [set-settings! (fn [value] (swap! state merge value))
        theme (get c/themes (get @tap-state ::c/theme ::c/nord))
        settings (merge theme @tap-state (assoc @state :depth 0 :set-settings! set-settings!))]
    [dnd/area
     settings
     [s/div
      {:style
       {:display :flex
        :flex-direction :column
        :background (::c/background settings)
        :color (::c/text settings)
        :font-family (:font/family settings)
        :font-size (:font-size settings)
        :height "100vh"
        :width "100vw"}}
      [scrollbars]
      [toolbar settings]
      [s/div {:style {:height "calc(100vh - 64px)" :width "100vw"}}
       [s/div
        {:style
         {:width "100%"
          :height "100%"
          :display :flex}}
        (when (contains? settings :portal/value)
          [inspect-1
           settings
           (:portal/value settings)])]]]]))
