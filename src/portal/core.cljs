(ns portal.core
  (:require [reagent.core :as r]
            [portal.styled :as s]
            [portal.colors :as c]
            [portal.rpc :as rpc]
            [portal.inspector :as ins :refer [inspector]]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [cognitect.transit :as t]))

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

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 1
    :limits/max-panes 1
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding "10px"
    :border-radius "2px"
    :portal/history '()}
   (:portal.themes/nord c/themes)))

(defonce state (r/atom default-settings))

(spec/def ::http-request
  (spec/keys
   :req-un [::url]
   :opt-un [::headers ::method ::body]))

(spec/def ::url string?)
(spec/def ::method #{"GET" "POST" "PUT" "PATCH" "DELETE"})
(spec/def ::headers map?)
(spec/def ::body string?)

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

(defn merge-state [new-state]
  (let [index (index-value (:portal/value new-state))
        new-state-with-index
        (assoc new-state :portal/index index)]
    (when (false? (:portal/open? (swap! state merge new-state-with-index)))
      (js/window.close))))

(defn load-state! []
  (-> (rpc/send!
       {:op             :portal.rpc/load-state
        :portal/state-id (:portal/state-id @state)})
      (.then merge-state)))

(defn clear-values! []
  (->
   (rpc/send! {:op :portal.rpc/clear-values})
   (.then #(swap! state assoc :portal/history '()))))

(defn on-nav [coll k v]
  (rpc/send! {:op :portal.rpc/on-nav :args [coll k v]}))

(defn http-request [request]
  (rpc/send! {:op :portal.rpc/http-request :request request}))

(defn table-view? [value]
  (and (coll? value) (every? map? value)))

(defn inspect-table [settings values]
  (let [columns (into #{} (mapcat keys values))
        background (ins/get-background settings)]
    [s/table
     {:style
      {:width "100%"
       :border-collapse :collapse
       :color (::c/text settings)
       :font-size  (:font-size settings)
       :border-radius (:border-radius settings)}}
     [s/tbody
      [s/tr
       (map-indexed
        (fn [grid-column column]
          [s/th {:key grid-column
                 :style
                 {:border (str "1px solid " (::c/border settings))
                  :background background
                  :box-sizing :border-box
                  :padding (:spacing/padding settings)}}
           [inspector (assoc settings :coll values) column]])
        columns)]
      (map-indexed
       (fn [grid-row row]
         [s/tr {:key grid-row}
          (map-indexed
           (fn [grid-column column]
             [s/td
              {:key grid-column
               :style
               {:border (str "1px solid " (::c/border settings))
                :background background
                :padding (:spacing/padding settings)
                :box-sizing :border-box}}
              (when (contains? row column)
                [inspector
                 (assoc settings :coll row :k column)
                 (get row column)])])
           columns)])
       values)]]))

(defn http-request? [value]
  (spec/valid? ::http-request value))

(defn inspect-http []
  (let [response (r/atom nil)]
    (fn [settings value]
      [s/div
       [s/button
        {:style (button-styles settings)
         :on-click (fn []
                     (->  (http-request value)
                          (.then #(reset! response (:response %)))))}
        "send request"]
       [inspector settings value]
       (when @response
         [inspector settings @response])])))

(defn inspect-metadata [settings value]
  (when-let [m (meta
                (if-not (t/tagged-value? value)
                  value
                  (.-rep value)))]
    [s/div
     {:style {:padding-bottom (:spacing/padding settings)}}
     [inspector (assoc settings :coll value :depth 0) m]]))

(def viewers
  {:portal.viewer/coll   {:predicate coll?         :component ins/inspect-coll}
   :portal.viewer/map    {:predicate map?          :component ins/inspect-map}
   :portal.viewer/table  {:predicate table-view?   :component inspect-table}
   :portal.viewer/http   {:predicate http-request? :component inspect-http}})

(defn inspect-1 []
  (let [selected-viewer (r/atom nil)]
    (fn [settings value]
      (let [compatible-viewers
            (into #{} (keep (fn [[k {:keys [predicate]}]]
                              (when (predicate value) k)) viewers))
            viewer    (or @selected-viewer (first compatible-viewers))
            component (when (contains? compatible-viewers viewer)
                        (get-in viewers [viewer :component] inspector))]
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
             :box-sizing :border-box
             :padding 20}}
           [s/div
            [inspect-metadata settings value]
            [inspector (assoc settings :component component) value]]]]
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
             {:value (pr-str viewer)
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
             (for [k compatible-viewers]
               [:option {:key k :value (pr-str k)} (pr-str k)])])
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

(defn app []
  (let [settings
        (assoc @state
               :depth 0
               :portal/on-clear #(clear-values!)
               :portal/on-nav
               (fn [target]
                 (-> (on-nav
                      (:coll target)
                      (:k target)
                      (:value target))
                     (.then #(when-not (= (:value %) (first (:history target)))
                               (swap! state
                                      assoc
                                      :portal/history
                                      (conj (:history target) (:value %)))))))
               :portal/on-back #(swap! state update :portal/history rest))]
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

(defn promise-loop [f]
  (.finally (f) #(promise-loop f)))

(defn render-app []
  (r/render [app]
            (.getElementById js/document "root")))

(defn main! []
  (promise-loop load-state!)
  (render-app))

(defn reload! [] (render-app))
