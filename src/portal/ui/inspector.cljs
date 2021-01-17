(ns portal.ui.inspector
  (:require ["react" :as react]
            [cognitect.transit :as t]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.ui.lazy :as l]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(def ^:private depth (react/createContext 0))

(defn- use-depth [] (react/useContext depth))

(defn inc-depth [& children]
  (into [:r> (.-Provider depth)
         #js {:value (inc (use-depth))}]
        children))

(defn date? [value] (instance? js/Date value))
(defn url? [value] (instance? js/URL value))
(defn bin? [value] (instance? js/Uint8Array value))

(defn get-value-type [value]
  (cond
    (instance? diff/Deletion value)   :diff
    (instance? diff/Insertion value)  :diff
    (instance? diff/Mismatch value)   :diff

    (bin? value)      :binary

    (map? value)      :map
    (set? value)      :set
    (vector? value)   :vector
    (list? value)     :list
    (coll? value)     :coll
    (boolean? value)  :boolean
    (symbol? value)   :symbol
    (number? value)   :number
    (string? value)   :string
    (keyword? value)  :keyword
    (var? value)      :var

    (uuid? value)     :uuid
    (t/uuid? value)   :uuid
    (url? value)      :uri
    (t/uri? value)    :uri
    (date? value)     :date

    (t/tagged-value? value)
    (case (.-tag value)
      "portal.transit/var"        :var
      "portal.transit/object"     :object
      "ratio"                     :ratio
      :tagged)))

(declare inspector)

(defn- diff-added [_settings value]
  (let [theme (theme/use-theme)
        color (::c/diff-add theme)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border [1 :solid color]
              :border-radius (:border-radius theme)}}
     [inspector _settings value]]))

(defn- diff-removed [_settings value]
  (let [theme (theme/use-theme)
        color (::c/diff-remove theme)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border [1 :solid color]
              :border-radius (:border-radius theme)}}
     [inspector _settings value]]))

(defn- inspect-diff [_settings value]
  (let [theme (theme/use-theme)
        removed (get value :- ::not-found)
        added   (get value :+ ::not-found)]
    [s/div
     {:style {:display :flex :width "100%"}}
     (when-not (= removed ::not-found)
       [s/div {:style
               {:flex 1
                :margin-right
                (when-not (= added ::not-found)
                  (:spacing/padding theme))}}
        [diff-removed _settings removed]])
     (when-not (= added ::not-found)
       [s/div {:style {:flex 1}}
        [diff-added _settings added]])]))

(defn get-background []
  (let [theme (theme/use-theme)]
    (if (even? (use-depth))
      (::c/background theme)
      (::c/background2 theme))))

(defn- tagged-tag [tag]
  (let [theme (theme/use-theme)]
    [s/div
     [s/span {:style {:color (::c/tag theme)}} "#"]
     tag]))

(defn- tagged-value [_settings tag value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:display :flex :align-items :center}}
     [tagged-tag tag]
     [s/div {:style {:margin-left (:spacing/padding theme)}}
      [s/div
       {:style {:margin (* -1 (:spacing/padding theme))}}
       [inspector _settings value]]]]))

(defn- preview-coll [open close]
  (fn [_settings value]
    (let [theme (theme/use-theme)]
      [s/div
       {:style
        {:color (::c/diff-remove theme)}}
       open
       (count value)
       (when (-> value meta :portal.runtime/more) "+")
       close])))

(def ^:private preview-map    (preview-coll "{" "}"))
(def ^:private preview-vector (preview-coll "[" "]"))
(def ^:private preview-list   (preview-coll "(" ")"))
(def ^:private preview-set    (preview-coll "#{" "}"))

(defn- preview-object [_settings value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/text theme)}}
     (:type (.-rep value))]))

(defn preview-tagged [_settings value]
  [tagged-tag (.-tag value)])

(defn- container-map [child]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:width "100%"
       :min-width :fit-content
       :display :grid
       :background (get-background)
       :grid-gap (:spacing/padding theme)
       :padding (:spacing/padding theme)
       :box-sizing :border-box
       :color (::c/text theme)
       :font-size  (:font-size theme)
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]}} child]))

(defn- container-map-k [child]
  [s/div {:style {:grid-column "1"
                  :display :flex
                  :align-items :center}} child])

(defn- container-map-v [child]
  [s/div {:style {:grid-column "2"
                  :display :flex
                  :align-items :center}} child])

(defn try-sort [values]
  (try (sort values)
       (catch :default _e values)))

(defn try-sort-map [values]
  (try (sort-by first values)
       (catch :default _e values)))

(defn- inspect-map [settings values]
  [container-map
   [l/lazy-seq
    settings
    (for [[k v] (try-sort-map values)]
      [:<>
       {:key (hash k)}
       [container-map-k
        [inspector (assoc settings :coll values) k]]
       [container-map-v
        [inspector (assoc settings :coll values :k k) v]]])]])

(defn- container-coll [child]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:width "100%"
       :text-align :left
       :display :grid
       :background (get-background)
       :grid-gap (:spacing/padding theme)
       :padding (:spacing/padding theme)
       :box-sizing :border-box
       :color (::c/text theme)
       :font-size  (:font-size theme)
       :border-radius (:border-radius theme)
       :border [1 :solid (::c/border theme)]}} child]))

(defn- inspect-coll [settings values]
  [container-coll
   [l/lazy-seq
    settings
    (map-indexed
     (fn [idx itm]
       ^{:key idx}
       [inspector (assoc settings :coll values :k idx) itm])
     values)]])

(defn- trim-string [string limit]
  (if-not (> (count string) limit)
    string
    (str (subs string 0 limit) "...")))

(defn- inspect-number [_settings value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}} value]))

(defn hex-color? [string]
  (re-matches #"#[0-9a-f]{6}|#[0-9a-f]{3}gi" string))

(defn- url-string? [string]
  (re-matches #"https?://.*" string))

(defn- inspect-string [_settings value]
  (let [theme (theme/use-theme)
        limit (:limits/string-length theme)]
    (cond
      (url-string? value)
      [s/span
       {:style {:color (::c/string theme)}}
       "\""
       [s/a
        {:href value
         :target "_blank"
         :style {:color (::c/string theme)}}
        (trim-string value limit)]
       "\""]

      (hex-color? value)
      [s/div
       {:style
        {:padding (* 0.65 (:spacing/padding theme))
         :box-sizing :border-box
         :background value}}
       [s/div
        {:style
         {:text-align :center
          :filter "contrast(500%) saturate(0) invert(1) contrast(500%)"
          :opacity 0.75
          :color value}}
        value]]

      :else
      [s/span {:style {:color (::c/string theme)}}
       (pr-str (trim-string value limit))])))

(defn- inspect-namespace [_settings value]
  (let [theme (theme/use-theme)]
    (when-let [ns (namespace value)]
      [s/span
       [s/span {:style {:color (::c/namespace theme)}} ns]
       [s/span {:style {:color (::c/text theme)}} "/"]])))

(defn- inspect-boolean [_settings value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/boolean theme)}}
     (pr-str value)]))

(defn- inspect-symbol [_settings value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/symbol theme) :white-space :nowrap}}
     [inspect-namespace _settings value]
     (name value)]))

(defn- inspect-keyword [_settings value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/keyword theme) :white-space :nowrap}}
     ":"
     [inspect-namespace _settings value]
     (name value)]))

(defn- inspect-date [_settings value]
  [tagged-value _settings "inst" (.toJSON value)])

(defn- inspect-uuid [_settings value]
  [tagged-value _settings "uuid" (str value)])

(defn- get-var-symbol [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (let [m (meta value)]
                              (symbol (name (:ns m)) (name (:name m))))))

(defn- inspect-var [_settings value]
  (let [theme (theme/use-theme)]
    [s/span
     [s/span {:style {:color (::c/tag theme)}} "#'"]
     [inspect-symbol _settings (get-var-symbol value)]]))

(defn- get-url-string [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (str value)))

(defn- inspect-uri [_settings value]
  (let [theme (theme/use-theme)
        value (get-url-string value)]
    [s/a
     {:href value
      :style {:color (::c/uri theme)}
      :target "_blank"}
     value]))

(defn- inspect-tagged [_settings value]
  [tagged-value _settings (.-tag value) (.-rep value)])

(defn- inspect-ratio [_settings value]
  (let [theme (theme/use-theme)
        [a b] (.-rep value)]
    [s/div
     [s/span {:style {:color (::c/number theme)}} (.-rep a)]
     "/"
     [s/span {:style {:color (::c/number theme)}} (.-rep b)]]))

(defn- inspect-default [_settings value]
  (let [theme (theme/use-theme)
        limit (:limits/string-length theme)]
    [s/span
     (trim-string (pr-str value) limit)]))

(defn- inspect-object [_settings value]
  (let [value (.-rep value)
        theme (theme/use-theme)
        limit (:limits/string-length theme)]
    [s/span {:title (:type value)
             :style
             {:color (::c/text theme)}}
     (trim-string (:string value) limit)]))

(defn- get-preview-component [type]
  (case type
    :diff       inspect-diff
    :map        preview-map
    :set        preview-set
    :vector     preview-vector
    :list       preview-list
    :coll       preview-list
    :boolean    inspect-boolean
    :symbol     inspect-symbol
    :number     inspect-number
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :object     preview-object
    :uri        inspect-uri
    :tagged     preview-tagged
    :ratio      inspect-ratio
    inspect-default))

(def ^:private preview-type?
  #{:map :set :vector :list :coll :tagged :object})

(defn preview [settings value]
  (let [type (get-value-type value)
        component (get-preview-component type)]
    (when (preview-type? type)
      [component settings value])))

(defn- get-inspect-component [type]
  (case type
    :diff       inspect-diff
    (:set :vector :list :coll) inspect-coll
    :map        inspect-map
    :boolean    inspect-boolean
    :symbol     inspect-symbol
    :number     inspect-number
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :object     inspect-object
    :uri        inspect-uri
    :tagged     inspect-tagged
    :ratio      inspect-ratio
    inspect-default))

(defn inspector [settings value]
  (let [theme (theme/use-theme)
        depth (use-depth)
        preview? (> depth (:limits/max-depth theme))
        type (get-value-type value)
        component (if preview?
                    (get-preview-component type)
                    (get-inspect-component type))
        nav-target? (= 1 depth)
        on-nav #(state/dispatch
                 settings
                 state/nav
                 (assoc settings :value value))]
    [s/div
     {:on-click
      (fn [e]
        (when nav-target?
          (.stopPropagation e)
          (on-nav)))
      :on-key-down
      (fn [e]
        (when (= (.-key e) "Enter")
          (.stopPropagation e)
          (on-nav)))
      :style {:width "100%"
              :padding (when (or preview? (not (coll? value)))
                         (* 0.65 (:spacing/padding theme)))
              :box-sizing :border-box
              :border-radius (:border-radius theme)
              :border [1 :solid "rgba(0,0,0,0)"]}
      :tab-index (when nav-target? 0)
      :style/hover {:border
                    (when nav-target?
                      [1 :solid "#D8DEE9"])}}
     [inc-depth [component settings value]]]))

(def viewer
  {:predicate (constantly true)
   :component inspector
   :name :portal.viewer/inspector})
