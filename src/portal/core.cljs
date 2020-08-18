(ns portal.core
  (:require [clojure.string :as str]
            [portal.colors :as c]
            [portal.drag-and-drop :as dnd]
            [portal.inspector :as ins :refer [inspector]]
            [portal.rpc :as rpc]
            [portal.styled :as s]
            [portal.viewer.diff :as d]
            [portal.viewer.hiccup :refer [inspect-hiccup]]
            [portal.viewer.html :refer [inspect-html]]
            [portal.viewer.image :as image]
            [portal.viewer.markdown :refer [inspect-markdown]]
            [portal.viewer.table :refer [inspect-table table-view?]]
            [portal.viewer.text :refer [inspect-text]]
            [portal.viewer.tree :refer [inspect-tree-1]]
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
  [{:name :portal.viewer/image    :predicate ins/bin?      :component image/inspect-image}
   {:name :portal.viewer/map      :predicate map?          :component ins/inspect-map}
   {:name :portal.viewer/coll     :predicate coll?         :component ins/inspect-coll}
   {:name :portal.viewer/table    :predicate table-view?   :component inspect-table}
   {:name :portal.viewer/tree     :predicate coll?         :component inspect-tree-1}
   {:name :portal.viewer/text     :predicate string?       :component inspect-text}
   {:name :portal.viewer/html     :predicate string?       :component inspect-html}
   {:name :portal.viewer/diff     :predicate d/can-view?   :component d/inspect-diff}
   {:name :portal.viewer/markdown :predicate string?       :component inspect-markdown}
   {:name :portal.viewer/hiccup   :predicate vector?       :component inspect-hiccup}])

(defn inspect-1 [settings value]
  (let [{:keys [selected-viewer set-settings!]} settings
        value (filter-data settings value)
        compatible-viewers (filter #((:predicate %) value) viewers)
        viewer    (or (some #(when (= (:name %) selected-viewer) %)
                            compatible-viewers)
                      (first compatible-viewers))
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
          :on-change #(set-settings!
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
                  {:transform "rotate(180deg)"}
                  (when disabled?
                    {:opacity 0.45
                     :cursor  :default}))}
      "►"])
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

(defonce state     (r/atom nil))
(defonce tap-state (r/atom nil))

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
       (cond
         (some? (:portal.rpc/exception settings))
         [ins/inspect-exception settings (:portal.rpc/exception settings)]

         :else
         [s/div
          {:style
           {:width "100%"
            :height "100%"
            :display :flex}}
          (when (contains? settings :portal/value)
            [inspect-1
             settings
             (:portal/value settings)])])]]]))

(defn render-app []
  (r/render [app]
            (.getElementById js/document "root")))

(defn on-back []
  (tap> ::on-back)
  (swap! state
         (fn [state]
           (if-let [previous-state (:portal/previous-state state)]
             (assoc previous-state :portal/next-state state)
             state))))

(defn on-forward []
  (tap> ::on-forward)
  (swap! state
         (fn [state]
           (if-let [next-state (:portal/next-state state)]
             (assoc next-state :portal/previous-state state)
             state))))

(defn on-nav [send! target]
  (tap> ::on-nav)
  (-> (send!
       {:op :portal.rpc/on-nav
        :args [(:coll target) (:k target) (:value target)]})
      (.then #(when-not (= (:value %) (:portal/value @state))
                (swap! state
                       (fn [state]
                         (assoc state
                                :portal/previous-state state
                                :portal/next-state nil
                                :search-text ""
                                :portal/value (:value %))))))))

(defn on-clear [send!]
  (tap> ::on-clear)
  (->
   (send! {:op :portal.rpc/clear-values})
   (.then #(swap! state
                  (fn [state]
                    (-> state
                        (dissoc :portal/value)
                        (assoc
                         :portal/previous-state nil
                         :portal/next-state nil)))))))

(defn merge-state [new-state]
  (let [theme (get c/themes (::c/theme new-state ::c/nord))
        merged-state (swap! tap-state merge new-state theme)]
    (when (false? (:portal/open? merged-state))
      (js/window.close))
    merged-state))

(defn load-state [send!]
  (-> (send!
       {:op              :portal.rpc/load-state
        :portal/state-id (:portal/state-id @tap-state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 1
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding 8
    :border-radius "2px"}))

(defn get-actions [send!]
  {:portal/on-clear (partial on-clear send!)
   :portal/on-nav   (partial on-nav send!)
   :portal/on-back  (partial on-back send!)
   :portal/on-forward (partial on-forward send!)
   :portal/on-load  (partial load-state send!)})

(defn long-poll []
  (let [on-load (or (:portal/on-load @state)
                    #(js/Promise.resolve false))]
    (.then (on-load)
           (fn [complete?]
             (when-not complete? (long-poll))))))

(defonce flush-send! (atom nil))

(defn ^:export start! [send!]
  (let [send! #(js/Promise.race
                [(.then (send! (rpc/edn->json %)) rpc/json->edn)
                 (js/Promise.
                  (fn [resolve] (reset! flush-send! resolve)))])
        settings (get-actions send!)]
    (when-let [f @flush-send!] (f))
    (when-not @state (long-poll))
    (swap! state merge default-settings settings)
    (render-app)))

(defn main!
  ([] (main! (get-actions rpc/send!)))
  ([settings]
   (when js/window.PORTAL_AUTO_START
     (swap! state merge default-settings settings)
     (long-poll)
     (render-app))))

(defn reload! [] (render-app))
