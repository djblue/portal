(ns portal.core
  (:require [clojure.string :as str]
            [cognitect.transit :as t]
            [portal.colors :as c]
            [portal.inspector :as ins :refer [inspector]]
            [portal.rpc :as rpc]
            [portal.styled :as s]
            [portal.viewer.diff :as d]
            [portal.viewer.hiccup :refer [inspect-hiccup]]
            [portal.viewer.html :refer [inspect-html]]
            [portal.viewer.markdown :refer [inspect-markdown]]
            [portal.viewer.table :refer [inspect-table table-view?]]
            [portal.viewer.text :refer [inspect-text]]
            [portal.viewer.tree :refer [inspect-tree-1]]
            [reagent.core :as r]))

(defn index-value
  ([value]
   (index-value nil [] value))

  ([coll path value]
   (cond
     (map? value)
     (mapcat
      (fn [[k v]]
        (let [path (conj path k)]
          (concat
           (index-value value path k)
           (index-value value path v))))
      value)

     (coll? value)
     (apply
      concat
      (map-indexed
       (fn [k v]
         (let [path (conj path k)]
           (concat
            (index-value value path k)
            (index-value value path v))))
       value))

     :else
     [{:path path
       :coll coll
       :k (last path)
       :value value
       :string-value (str/lower-case (pr-str value))}])))

(defn filter-index [index s]
  (filter #(str/includes? (:string-value %)
                          (str/lower-case s))
          index))

(defonce show-meta? (r/atom false))

(defn inspect-metadata [settings value]
  (when-let [m (meta
                (if-not (t/tagged-value? value)
                  value
                  (.-rep value)))]
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
      [s/a
       {:href "https://clojure.org/reference/metadata"
        :target "_blank"
        :style
        {:color :inherit
         :text-decoration :none}}
       "metadata"]
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
  [{:name :portal.viewer/map      :predicate map?          :component ins/inspect-map}
   {:name :portal.viewer/coll     :predicate coll?         :component ins/inspect-coll}
   {:name :portal.viewer/table    :predicate table-view?   :component inspect-table}
   {:name :portal.viewer/tree     :predicate coll?         :component inspect-tree-1}
   {:name :portal.viewer/text     :predicate string?       :component inspect-text}
   {:name :portal.viewer/html     :predicate string?       :component inspect-html}
   {:name :portal.viewer/diff     :predicate d/can-view?   :component d/inspect-diff}
   {:name :portal.viewer/markdown :predicate string?       :component inspect-markdown}
   {:name :portal.viewer/hiccup   :predicate vector?       :component inspect-hiccup}])

(defn inspect-1 []
  (let [selected-viewer (r/atom nil)]
    (fn [settings value]
      (let [compatible-viewers (filter #((:predicate %) value) viewers)
            viewer       (or (some #(when (= (:name %) @selected-viewer) %)
                                   compatible-viewers)
                             (first compatible-viewers))
            component    (or (:component viewer) inspector)
            settings (assoc settings
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
            :box-sizing :border-box
            :border (str "1px solid " (::c/border settings))}}
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
            :min-height 64
            :align-items :center
            :justify-content :space-between
            :background (::c/background2 settings)}}
          (if (empty? compatible-viewers)
            [s/div]
            [:select
             {:value (pr-str (:name viewer))
              :on-change #(reset! selected-viewer
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
           [ins/preview settings value]]]]))))

(defonce search-text (r/atom ""))

(defn search-input [settings]
  [s/input
   {:on-change #(reset! search-text (.-value (.-target %)))
    :value @search-text
    :placeholder "Type to filter..."
    :style
    {:flex "1"
     :background (::c/background settings)
     :margin (:spacing/padding settings)
     :padding (:spacing/padding settings)
     :box-sizing :border-box
     :font-size (:font-size settings)
     :color (::c/text settings)
     :border (str "1px solid " (::c/border settings))}}])

(defn search-results [settings]
  (let [search-text-value @search-text]
    (when-not (str/blank? search-text-value)
      [inspect-1
       (update settings
               :portal/on-nav
               (fn [on-nav]
                 #(do
                    (-> (on-nav %)
                        (.then (fn [] (reset! search-text nil)))))))
       (->>
        search-text-value
        (filter-index (:portal/index settings))
        (map #(dissoc % :string-value))
        (take 15))])))

(defn button-styles [settings]
  {:background (::c/text settings)
   :color (::c/background settings)
   :font-size (:font-size settings)
   :border :none
   :box-sizing :border-box
   :padding "10px 20px"
   :border-radius (:border-radius settings)
   :cursor :pointer
   :margin "0 20px"})

(defn toolbar [settings]
  [s/div
   {:style
    {:height "64px"
     :flex-direction :row
     :background (::c/background2 settings)
     :display :flex
     :align-items :center
     :justify-content :center}}
   [s/button
    {:on-click (:portal/on-back settings)
     :style    (button-styles settings)} "back"]
   [search-input settings]
   [s/button
    {:on-click (:portal/on-clear settings)
     :style    (button-styles settings)} "clear"]])

(defn get-history-stack [settings]
  (if (empty? (:portal/history settings))
    [(list (:portal/value settings))]
    (loop [ls (:portal/history settings) result []]
      (if (empty? ls)
        result
        (recur (rest ls) (conj result ls))))))

(defonce state (r/atom nil))

(defn app []
  (let [settings (assoc @state :depth 0)]
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

        (not (str/blank? @search-text))
        [search-results settings]

        :else
        [s/div
         {:style
          {:width "100%"
           :height "100%"
           :display :flex}}
         (->>
          (get-history-stack settings)
          (take (:limits/max-panes settings))
          reverse
          (map-indexed
           (fn [idx ls]
             [:<>
              {:key idx}
              [inspect-1
               (-> settings
                   (assoc :coll (second ls))
                   (update
                    :portal/on-nav
                    (fn [on-nav] #(on-nav (assoc % :history ls)))))
               (first ls)]])))])]]))

(defn render-app []
  (r/render [app]
            (.getElementById js/document "root")))

(defn promise-loop [f]
  (.then
   (f)
   (fn [complete?]
     (when-not complete? (promise-loop f)))))

(defn on-back []
  (swap! state update :portal/history rest))

(defn on-nav [send! target]
  (->  (send!
        {:op :portal.rpc/on-nav
         :args [(:coll target) (:k target) (:value target)]})
       (.then #(when-not (= (:value %) (first (:history target)))
                 (swap! state
                        assoc
                        :portal/history
                        (conj (:history target) (:value %)))))))

(defn on-clear [send!]
  (->
   (send! {:op :portal.rpc/clear-values})
   (.then #(swap! state assoc :portal/history '()))))

(defn merge-state [new-state]
  (let [index (index-value (:portal/value new-state))
        new-state-with-index
        (assoc new-state :portal/index index)]
    (when (false? (:portal/open? (swap! state merge new-state-with-index)))
      (js/window.close))
    new-state-with-index))

(defn load-state [send!]
  (-> (send!
       {:op             :portal.rpc/load-state
        :portal/state-id (:portal/state-id @state)})
      (.then merge-state)
      (.then #(:portal/complete? %))))

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 1
    :limits/max-panes 1
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding 8
    :border-radius "2px"
    :portal/history '()}
   (:portal.themes/nord c/themes)))

(defn get-actions [send!]
  {:portal/on-clear (partial on-clear send!)
   :portal/on-nav   (partial on-nav send!)
   :portal/on-back  (partial on-back send!)
   :portal/on-load  (partial load-state send!)})

(defn main!
  ([] (main! (get-actions rpc/send!)))
  ([settings]
   (swap! state merge default-settings settings)
   (promise-loop (:portal/on-load settings))
   (render-app)))

(defn reload! []
  ((:portal/on-load @state))
  (render-app))
