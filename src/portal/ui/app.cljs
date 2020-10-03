(ns portal.ui.app
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.state :refer [state tap-state]]
            [portal.ui.drag-and-drop :as dnd]
            [portal.ui.inspector :as ins :refer [inspector]]
            [portal.ui.styled :as s]
            [portal.ui.viewer.diff :as d]
            [portal.ui.viewer.edn :refer [inspect-edn edn?]]
            [portal.ui.viewer.exception :as ex]
            [portal.ui.viewer.hiccup :refer [inspect-hiccup]]
            [portal.ui.viewer.html :refer [inspect-html]]
            [portal.ui.viewer.image :as image]
            [portal.ui.viewer.json :refer [inspect-json json?]]
            [portal.ui.viewer.markdown :refer [inspect-markdown]]
            [portal.ui.viewer.table :refer [inspect-table table-view?]]
            [portal.ui.viewer.text :refer [inspect-text]]
            [portal.ui.viewer.transit :refer [inspect-transit transit?]]
            [portal.ui.viewer.tree :refer [inspect-tree-1]]
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
  [{:name :portal.viewer/ex       :predicate ex/exception? :component ex/inspect-exception}
   {:name :portal.viewer/image    :predicate ins/bin?      :component image/inspect-image}
   {:name :portal.viewer/map      :predicate map?          :component ins/inspect-map}
   {:name :portal.viewer/coll     :predicate coll?         :component ins/inspect-coll :datafy seq}
   {:name :portal.viewer/table    :predicate table-view?   :component inspect-table}
   {:name :portal.viewer/tree     :predicate coll?         :component inspect-tree-1}
   {:name :portal.viewer/text     :predicate string?       :component inspect-text}
   {:name :portal.viewer/json     :predicate json?         :component inspect-json}
   {:name :portal.viewer/edn      :predicate edn?          :component inspect-edn}
   {:name :portal.viewer/transit  :predicate transit?      :component inspect-transit}
   {:name :portal.viewer/html     :predicate string?       :component inspect-html}
   {:name :portal.viewer/diff     :predicate d/can-view?   :component d/inspect-diff}
   {:name :portal.viewer/markdown :predicate string?       :component inspect-markdown}
   {:name :portal.viewer/hiccup   :predicate vector?       :component inspect-hiccup}])

(defn get-viewer [settings value]
  (let [selected-viewer    (:selected-viewer settings)
        compatible-viewers (filter #((:predicate %) value) viewers)]
    {:compatible-viewers compatible-viewers
     :viewer
     (or (some #(when (= (:name %) selected-viewer) %)
               compatible-viewers)
         (first compatible-viewers))}))

(defn get-datafy [settings]
  (fn [value]
    (try
      ((get-in (get-viewer settings (:portal/value settings)) [:viewer :datafy] identity)
       (filter-data settings value))
      (catch js/Error _e value))))

(defn inspect-1 [settings value]
  (let [value (filter-data settings value)
        {:keys [compatible-viewers viewer]} (get-viewer settings value)
        component (or (:component viewer) inspector)
        settings  (assoc settings
                         :portal/rainbow
                         (cycle ((juxt ::c/exception ::c/keyword ::c/string
                                       ::c/tag ::c/number ::c/uri) settings)))]
    [s/div
     {:style
      {:flex 1}}
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
         [inspector (assoc settings :component component) value]]]]]
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
          :on-change #((:set-settings! settings)
                       {:selected-viewer
                        (keyword (.substr (.. % -target -value) 1))})
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
       :title    "go back"
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
       :title    "go next"
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
       :title    "clear portal"
       :on-click (:portal/on-clear settings)
       :style    (merge
                  (button-styles settings)
                  (when disabled?
                    {:opacity 0.45
                     :cursor  :default})
                  {:padding-left (* 2 (:spacing/padding settings))
                   :padding-right (* 2 (:spacing/padding settings))})}
      "clear"])])

(defn app []
  (let [set-settings! (fn [value] (swap! state merge value))
        settings (merge @tap-state (assoc @state :depth 0 :set-settings! set-settings!))]
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
