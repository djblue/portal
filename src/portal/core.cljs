(ns portal.core
  (:require [reagent.core :as r]
            [portal.styled :as s]
            [portal.colors :as c]
            [portal.rpc :as rpc]
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
       :key (last path)
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
    :limits/max-depth 2
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

(declare portal)

(defn get-background [settings]
  (if (even? (:depth settings))
    (::c/background settings)
    (::c/background2 settings)))

(defn summary [settings value]
  (cond
    (coll? value)
    (when-let [[open close]
               (cond
                 (map? value)     ["{" "}"]
                 (vector? value)  ["[" "]"]
                 (seq? value)     ["(" ")"]
                 (set? value)     ["#{" "}"])]
      [s/div
       {:style {:vertical-align :top
                :color "#bf616a"}}
       open (count value) close])

    (t/tagged-value? value)
    (let [tag (.-tag value) rep (.-rep value)]
      (case tag
        ("r"
         "portal.transit/var"
         "portal.transit/exception") nil

        "portal.transit/unknown"
        [s/span {:style {:color (::c/text settings)}} (:type rep)]

        [s/div
         {:style {:box-sizing :border-box
                  :padding (:spacing/padding settings)}}
         [s/span {:style {:color (::c/tag settings)}} "#"]
         tag]))))

(defn table-view? [value]
  (and (coll? value) (every? map? value)))

(defn portal-table [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      (let [columns (into #{} (mapcat keys values))
            background (get-background settings)]
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
               [portal settings column]])
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
                    [portal settings (get row column)])])
               columns)])
           values)]]))))

(defn http-request? [value]
  (spec/valid? ::http-request value))

(defn portal-http []
  (let [response (r/atom nil)]
    (fn [settings value]
      [s/div
       [s/button
        {:style (button-styles settings)
         :on-click (fn []
                     (->  (http-request value)
                          (.then #(reset! response (:response %)))))}
        "send request"]
       [portal settings value]
       (when @response
         [portal settings @response])])))

(defn portal-map [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      [s/div
       {:style
        {:width "100%"
         :display :grid
         :background (get-background settings)
         :grid-gap (:spacing/padding settings)
         :padding (:spacing/padding settings)
         :box-sizing :border-box
         :color (::c/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (::c/border settings))}}
       (take
        (:limits/max-length settings)
        (filter
         some?
         (for [[k v] values]
           (let [portal-k [portal settings k]
                 portal-v [portal settings v]]
             [:<>
              {:key (hash k)}
              [s/div {:style
                      {:text-align :left
                       :grid-column "1"}}
               [s/div
                {:style {:display :flex}}
                portal-k]]
              [s/div {:style
                      {:grid-column "2"
                       :text-align :right}}

               portal-v]]))))])))

(defn portal-coll [settings values]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [summary settings values]
      [s/div
       {:key (hash values)
        :style
        {:text-align :left
         :display :grid
         :background (get-background settings)
         :grid-gap (:spacing/padding settings)
         :padding (:spacing/padding settings)
         :box-sizing :border-box
         :color (::c/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (::c/border settings))}}
       (->> values
            (map-indexed
             (fn [idx itm]
               ^{:key idx}
               [portal settings itm]))
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
          i    (str/index-of (str/lower-case value)
                             (str/lower-case text))]
      (when-not (nil? i)
        (let [before  (subs value 0 i)
              match   (subs value i (+ len i))
              after   (subs value (+ len i))]
          [s/span before [:mark match] after])))))

(def viewers
  {:portal.viewer/coll   {:predicate coll?         :component portal-coll}
   :portal.viewer/map    {:predicate map?          :component portal-map}
   :portal.viewer/table  {:predicate table-view?   :component portal-table}
   :portal.viewer/http   {:predicate http-request? :component portal-http}})

(defn portal-number [settings value]
  [s/span {:style {:color (::c/number settings)}} value])

(defn hex-color? [s]
  (re-matches #"#[0-9a-f]{6}|#[0-9a-f]{3}gi" s))

(defn portal-string [settings value]
  (if-let [color (hex-color? value)]
    [s/div
     {:style
      {:padding (:spacing/padding settings)
       :box-sizing :border-box
       :background color}}
     [s/div
      {:style
       {:text-align :center
        :filter "contrast(500%) saturate(0) invert(1) contrast(500%)"
        :opacity 0.75
        :color color}}
      color]]

    [s/span {:style {:color (::c/string settings)}}
     (pr-str (trim-string settings value))]))

(defn portal-namespace [settings value]
  (when-let [ns (namespace value)]
    [s/span {:style {:color (::c/namespace settings)}} ns "/"]))

(defn portal-symbol [settings value]
  [s/span {:style {:color (::c/symbol settings) :white-space :nowrap}}
   [portal-namespace settings value]
   (name value)])

(defn portal-keyword [settings value]
  [s/span {:style {:color (::c/keyword settings) :white-space :nowrap}}
   ":"
   [portal-namespace settings value]
   (name value)])

(defn portal-var [settings value]
  [s/span
   [s/span {:style {:color (::c/tag settings)}} "#'"]
   [portal-symbol settings value]])

(defn portal-uri [settings value]
  [s/a
   {:href value
    :style {:color (::c/uri settings)}
    :target "_blank"}
   value])

(defn portal-exception [settings value]
  (let [settings (update settings :depth inc)]
    (if (> (:depth settings) (:limits/max-depth settings))
      [s/span {:style {:font-weight :bold
                       :color (::c/exception settings)}}
       (:class-name (first value))]
      [s/div
       {:style
        {:background (get-background settings)
         :padding (:spacing/padding settings)
         :box-sizing :border-box
         :color (::c/text settings)
         :font-size  (:font-size settings)
         :border-radius (:border-radius settings)
         :border (str "1px solid " (::c/border settings))}}
       (map
        (fn [value]
          (let [{:keys [class-name message stack-trace]} value]
            [:<>
             {:key (hash value)}
             [s/div
              {:style
               {:margin-bottom (:spacing/padding settings)}}
              [s/span {:style {:font-weight :bold
                               :color (::c/exception settings)}}
               class-name] ": " message]
             [s/div
              {:style {:display     :grid
                       :text-align :right}}
              (map-indexed
               (fn [idx line]
                 (let [{:keys [names]} line]
                   [:<> {:key idx}
                    (if (:omitted line)
                      [s/div {:style {:grid-column "1"}} "..."]
                      [s/div {:style {:grid-column "1"}}
                       (if (empty? names)
                         [s/span
                          [s/span {:style {:color (::c/package settings)}}
                           (:package line) "."]
                          [s/span {:style {:font-style :italic}}
                           (:simple-class line) "."]
                          (str (:method line))]
                         (let [[ns & names] names]
                           [s/span
                            [s/span {:style {:color (::c/namespace settings)}} ns "/"]
                            (str/join "/" names)]))])
                    [s/div {:style {:grid-column "2"}} (:file line)]
                    [s/div {:style {:grid-column "3"}}
                     [portal-number settings (:line line)]]]))
               stack-trace)]]))
        value)])))

(defn portal [settings value]
  [s/div
   {:on-click
    (fn [e]
      (when (= 1 (:depth settings))
        (.stopPropagation e)
        ((:portal/on-nav settings) {:value value})))
    :style {:cursor :pointer
            :width "100%"
            :border-radius (:border-radius settings)
            :border "1px solid rgba(0,0,0,0)"}
    :style/hover {:border
                  (when (= 1 (:depth settings))
                    "1px solid #D8DEE9")}}
   (cond
     (map? value)
     [portal-map settings value]

     (coll? value)
     [portal-coll settings value]

     (boolean? value)
     [s/span {:style {:color (::c/boolean settings)}}
      (pr-str value)]

     (symbol? value)
     [portal-symbol settings value]

     (number? value)
     [portal-number settings value]

     (string? value)
     [portal-string settings value]

     (keyword? value)
     [portal-keyword settings value]

     (instance? js/Date value)
     [s/span {:style {:color (::c/date settings)}}
      (pr-str value)]

     (instance? cljs.core/UUID value)
     [s/span {:style {:color (::c/uuid settings)}}
      (pr-str value)]

     (t/tagged-value? value)
     (let [tag (.-tag value) rep (.-rep value)]
       (case tag
         "portal.transit/var"
         [portal-var settings rep]
         "portal.transit/exception"
         [portal-exception settings rep]
         "portal.transit/unknown"
         [s/span {:title (:type rep)
                  :style
                  {:color (::c/text settings)}}
          (:string rep)]

         "r"
         [portal-uri settings (.-rep value)]

         [s/div
          {:style {:display :flex
                   :align-items :center}}
          [s/div
           {:style {:padding (:spacing/padding settings)}}
           [s/span {:style {:color (::c/tag settings)}} "#"]
           tag]
          [s/div
           {:style {:flex 1}}
           [portal settings rep]]]))

     :else
     [s/span {}
      (trim-string settings (pr-str value))])])

(defn portal-metadata [settings value]
  (when-let [m (meta
                (if-not (t/tagged-value? value)
                  value
                  (.-rep value)))]
    [s/div
     {:style {:padding-bottom (:spacing/padding settings)}}
     [portal settings m]]))

(defn portal-1 []
  (let [selected-viewer (r/atom nil)]
    (fn [settings value]
      (let [compatible-viewers
            (into #{} (keep (fn [[k {:keys [predicate]}]]
                              (when (predicate value) k)) viewers))
            viewer    (or @selected-viewer (first compatible-viewers))
            component (if-not (contains? compatible-viewers viewer)
                        portal
                        (get-in viewers [viewer :component] portal))]
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
            [portal-metadata settings value]
            [component settings value]]]]
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
           [summary settings value]]]]))))

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
      [portal-1
       (update settings
               :portal/on-nav
               (fn [on-nav]
                 #(do
                    (-> (on-nav (:value %))
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
                      (:key target)
                      (:value target))
                     (.then #(swap! state
                                    assoc
                                    :portal/history
                                    (conj (:history target) (:value %))))))
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
        [portal-exception settings (.-rep (:portal.rpc/exception settings))]

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
              [portal-1
               (update settings
                       :portal/on-nav
                       (fn [on-nav] #(on-nav (assoc % :coll (first ls) :history ls))))
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
