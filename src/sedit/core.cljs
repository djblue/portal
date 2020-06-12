(ns sedit.core
  (:require [reagent.core :as r]
            [clojure.string :as s]
            [cognitect.transit :as t]))

(defn index-value
  ([value]
   (index-value [] value))

  ([path value]
   (cond
     (map? value)
     (mapcat
      (fn [[k v]]
        (let [path (conj path k)]
          (concat
           (index-value path k)
           (index-value path v))))
      value)

     (coll? value)
     (apply
      concat
      (map-indexed
       (fn [k v]
         (let [path (conj path k)]
           (concat
            (index-value path k)
            (index-value path v))))
       value))

     :else
     [{:path path
       :value value
       :string-value (s/lower-case (pr-str value))}])))

(defn filter-index [index s]
  (filter #(s/includes? (:string-value %)
                        (s/lower-case s))
          index))

(declare sedit)

(defn collapsible []
  (let [state (r/atom {:open? true})]
    (fn [props child]
      [:div
       {:on-click #(do
                     (swap! state update :open? not)
                     (.stopPropagation %))}
       (let [{:keys [open?]} @state]
         (if-not open?
           (:hidden props)
           child))])))

(defn get-background [settings]
  (if (even? (:depth settings))
    (:colors/background settings)
    (:colors/background2 settings)))

(defn summary [settings value]
  (when-let [[open close]
             (cond
               (map? value)     ["{" "}"]
               (vector? value)  ["[" "]"]
               (seq? value)     ["(" ")"]
               (set? value)     ["#{" "}"])]
    [:div
     {:style {:vertical-align :top
              :color "#bf616a"}}
     open (count value) close]))

(defn table-view? [value]
  (and (coll? value) (every? map? value)))

(defn table-view [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      (let [columns (into #{} (mapcat keys values))]
        [:table
         {:style
          {:width "100%"
           :display :grid
           :background (get-background settings)
           :border-collapse :collapse
           :color (:colors/text settings)
           :font-size  (:font-size settings)
           :border-radius (:border-radius settings)}}
         [:tbody
          [:tr
           (map-indexed
            (fn [grid-column column]
              [:th {:key grid-column
                    :style
                    {:border (str "1px solid " (:colors/border settings))
                     :box-sizing :border-box
                     :padding (:spacing/padding settings)}}
               [sedit settings column]])
            columns)]
          (map-indexed
           (fn [grid-row row]
             [:tr {:key grid-row}
              (map-indexed
               (fn [grid-column column]
                 [:td
                  {:key grid-column
                   :style
                   {:border (str "1px solid " (:colors/border settings))
                    :padding (:spacing/padding settings)
                    :box-sizing :border-box}}
                  (when-let [value (get row column)]
                    [sedit settings value])])
               columns)])
           values)]]))))

(defonce path (r/atom []))

(defn sedit-map [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      [:div
       {:style
        {:width "100%"
         :display :grid
         :background (get-background settings)
         :grid-gap (:spacing/padding settings)
         :padding (:spacing/padding settings)
         :box-sizing :border-box
         :color (:colors/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (:colors/border settings))}}

       #_[:thead
          [:tr
           {:style
            {:border-bottom (str "1px solid " (:colors/border settings))}}
           [:td
            [:div
             {:style {:display :flex}}
             #_(map (partial sedit settings) (:parent-path settings))
             type+count]]
           [:td]]]
       (take
        (:limits/max-length settings)
        (filter
         some?
         (for [[k v] values]
           (let [sedit-k [sedit settings k]
                 sedit-v [sedit (update settings :parent-path conj k) v]]
             [:<>
              {:key (hash k)}
              [:div {:on-click #(reset! path (conj (:parent-path settings) k))
                     :style
                     {:cursor :pointer
                      :grid-column 1}}
               [:div
                {:style {:display :flex}}
                sedit-k]]

              #_[:td {:style
                      {:vertical-align :top
                       :text-align :left
                       :padding (:spacing/padding settings)}}
                 [summary settings v]]

              [:div {:style
                     {:grid-column 2
                      :text-align :right}}

               sedit-v]]))))])))

(defn sedit-coll [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      [:div
       {:key (hash values)
        :style
        {:text-align :left
         :display :grid
         :background (get-background settings)
         :grid-gap (:spacing/padding settings)
         :padding (:spacing/padding settings)
         :box-sizing :border-box
         :color (:colors/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (:colors/border settings))}}

       #_[:div
          {:style
           {:border-bottom (str "1px solid " (:colors/border settings))}}
          type+count]
       (->> values
            (map-indexed
             (fn [idx itm]
               ^{:key idx}
               [sedit (update settings :parent-path conj idx) itm]))
            (filter some?)
            (take (:limits/max-length settings)))])))

(defn trim-string [settings s]
  (let [max-length (:limits/string-length settings)]
    (if-not (> (count s) max-length)
      s
      (str (subs s 0 max-length) "..."))))

(defn text-search [settings value]
  (cond
    (nil? (:input/text-search settings)) value

    (some? value)
    (let [text (:input/text-search settings)
          len  (count text)
          i    (s/index-of (s/lower-case value)
                           (s/lower-case text))]
      (when-not (nil? i)
        (let [before  (subs value 0 i)
              match   (subs value i (+ len i))
              after   (subs value (+ len i))]
          [:span before [:mark match] after])))))

(defn sedit [settings value]
  (cond
    (table-view? value)
    [table-view settings value]

    (map? value)
    [sedit-map settings value]

    (coll? value)
    [sedit-coll settings value]

    (boolean? value)
    [:span {:style {:color (:colors/boolean settings)}}
     (pr-str value)]

    (symbol? value)
    [:span {:style {:color (:colors/symbol settings)}}
     value]

    (number? value)
    [:span {:style {:color (:colors/number settings)}}
     value]

    (string? value)
    [:span {:style {:color (:colors/string settings)}}
     (pr-str (trim-string settings value))]

    (keyword? value)
    (let [keyword-name (name value)
          keyword-namespace (namespace value)]
      (when keyword-name
        [:span {:style {:color (:colors/keyword settings) :white-space :nowrap}}
         ":" (when keyword-namespace
               [:span {:style {:color (:colors/keyword-namespace settings)}}
                keyword-namespace
                "/"])
         keyword-name]))

    (instance? js/Date value)
    [:span {:style {:color (:colors/date settings)}}
     (pr-str value)]

    (instance? cljs.core/UUID value)
    [:span {:style {:color (:colors/uuid settings)}}
     (pr-str value)]

    (instance? cljs.core/Var value)
    [:span {:style {:color (:colors/var settings)}}
     (pr-str value)]

    :else
    [:span {}
     (trim-string settings (pr-str value))]))

(def themes
  {:themes/nord
   {:colors/text "#d8dee9"
    :colors/background "#2e3440"
    :colors/background2 "rgba(0,0,0,0.1)"
    :colors/boolean "#5e81ac"
    :colors/string "#a3be8c"
    :colors/keyword "#5e81ac"
    :colors/keyword-namespace "#88c0d0"
    :colors/symbol "#d8dee9"
    :colors/number "#b48ead"
    :colors/date "#ebcb8b"
    :colors/uuid "#d08770"
    :colors/var "#88c0d0"
    :colors/border "#4c566a"}})

(def default-settings
  (merge
   {:font/family "monospace"
    :font-size "12pt"
    :limits/string-length 100
    :limits/max-depth 3
    :limits/max-length 1000
    :layout/direction :row
    :spacing/padding "10px"
    :border-radius "2px"}
   (:themes/nord themes)))

(comment
  (true? (swap! state assoc :sedit/value example))
  (true? (swap! state assoc :spacing/padding "10px")))

(def example
  {:example/booleans #{true false}
   :example/nil nil
   :example/vector [1 2 4]
   "string-key" "string-value"
   :example/list (list 1 2 3)
   :example/set #{1 2 3}
   {:example/settings default-settings} :hello-world
   #{1 2 3} [4 5 6]
   :example/date (js/Date.)
   :example/var #'default-settings
   :example/uuid (random-uuid)
   :example/nested-vector [1 2 3 [4 5 6]]
   :example/code '(defn hello-world [] (println "hello, world"))})

(defonce state (r/atom default-settings))

(defonce search-text (r/atom ""))

(defn search-input [settings]
  [:input
   {:on-change #(reset! search-text (.-value (.-target %)))
    :value @search-text
    :style
    {:flex "1"
     :background (:colors/background settings)
     :margin (:spacing/padding settings)
     :padding (:spacing/padding settings)
     :box-sizing :border
     :font-size (:font-size settings)
     :color (:colors/text settings)
     :border (str "1px solid " (:colors/border settings))}}])

(defn search-results [settings]
  (let [search-text-value @search-text]
    (when-not (s/blank? search-text-value)
      [:<>
       (->>
        search-text-value
        (filter-index (:sedit/index settings))
        (take 10)
        (map-indexed
         (fn [index item]
           [:div
            {:key index :on-click #(do
                                     (reset! search-text nil)
                                     (reset! path (:path item)))}
            [sedit settings (dissoc item :string-value)]])))])))

(defn toolbar [settings path]
  [:div
   {:style
    {:height "64px"
     :flex-direction :row
     :display :flex
     :align-items :center
     :justify-content :center
     :border-bottom  (str "1px solid " (:colors/border settings))}}
   [:button
    {:on-click #(swap! path (fn [v] (if (empty? v) v (pop v))))
     :style
     {:background (:colors/text settings)
      :color (:colors/background settings)
      :font-size (:font-size settings)
      :border :none
      :box-sizing :border-box
      :padding "10px 20px"
      :border-radius (:border-radius settings)
      :cursor :pointer
      :margin "0 20px"}} "back"]
   [search-input settings]])

(defn app []
  (let [settings    @state
        value       (:sedit/value settings)
        parent-path @path
        settings    (assoc settings :parent-path parent-path)
        settings    (assoc settings :depth 0)]
    [:div
     {:style
      {:display :flex
       :justify-content :space-between
       :flex-direction :column
       :background (:colors/background settings)
       :color (:colors/text settings)
       :font-family (:font/family settings)
       :font-size (:font-size settings)
       :height "100vh"
       :width "100vw"}}
     [toolbar settings path]
     [:div
      {:style
       {:display :flex
        :flex 1
        :align-items :center
        :justify-content :center
        :overflow-y :auto}}
      [:div {:style {:max-height "100%" :max-width "100%"}}
       [:div {:style {:padding "64px" :box-sizing :border-box}}
        (if-not (s/blank? @search-text)
          [search-results settings]
          [sedit settings (get-in value parent-path)])]]]
     #_[sedit (-> settings
                  (dissoc :input/text-search)
                  (assoc :layout/direction :row))
        (select-keys
         settings
         [:limits/string-length
          :limits/max-depth
          :limits/max-length
          :parent-path])]]))

(defn json->edn [json]
  (let [r (t/reader :json)]
    (t/read r json)))

(defn edn->json [edn]
  (let [w (t/writer :json)]
    (t/write w edn)))

(defn send-rpc!
  ([msg] (send-rpc! msg identity))
  ([msg done]
   (-> (js/fetch
        "/rpc"
        #js {:method "POST" :body (edn->json msg)})
       (.then #(.text %))
       (.then json->edn)
       (.then done))))

(defn merge-state [new-state]
  (let [index (index-value (:sedit/value new-state))
        new-state-with-index
        (assoc new-state :sedit/index index)]
    (swap! state merge new-state-with-index)))

(def load-state!
  (partial send-rpc! {:op :sedit.rpc/load-state} merge-state))

(def await-state!
  (partial send-rpc!  {:op :sedit.rpc/await-state} merge-state))

(defn promise-loop [f]
  (.finally (f) #(promise-loop f)))

(defn render-app []
  (r/render [app]
            (.getElementById js/document "root")))

(defn main! []
  (load-state!)
  (promise-loop await-state!)
  (render-app))

(defn reload! [] (render-app))
