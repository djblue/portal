(ns portal.ui.inspector
  (:require ["react" :as react]
            [cognitect.transit :as t]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.ui.lazy :as l]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(def ^:private inspector-context (react/createContext {:depth 0}))

(defn use-context [] (react/useContext inspector-context))

(defn with-depth [& children]
  (let [context (use-context)]
    (into [:r> (.-Provider inspector-context)
           #js {:value (assoc context :depth 0)}] children)))

(defn inc-depth [& children]
  (let [context (use-context)]
    (into [:r> (.-Provider inspector-context)
           #js {:value (update context :depth inc)}]
          children)))

(defn- use-depth [] (:depth (use-context)))

(defn with-context [value & children]
  (let [context (use-context)]
    (into
     [:r> (.-Provider inspector-context)
      #js {:value (merge context value)}] children)))

(defn with-collection [coll & children]
  (into [with-context {:collection coll :key nil}] children))

(defn with-key [k & children]
  (let [context (use-context)
        path    (get context :path [])]
    (into [with-context {:key k :path (conj path k)}] children)))

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
(declare preview)

(defn- diff-added [value]
  (let [theme (theme/use-theme)
        color (::c/diff-add theme)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border [1 :solid color]
              :border-radius (:border-radius theme)}}
     [inspector value]]))

(defn- diff-removed [value]
  (let [theme (theme/use-theme)
        color (::c/diff-remove theme)]
    [s/div
     {:style {:flex 1
              :background (str color "22")
              :border [1 :solid color]
              :border-radius (:border-radius theme)}}
     [inspector value]]))

(defn- inspect-diff [value]
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
        [diff-removed removed]])
     (when-not (= added ::not-found)
       [s/div {:style {:flex 1}}
        [diff-added added]])]))

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

(defn- tagged-value [tag value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:display :flex :align-items :center}}
     [tagged-tag tag]
     [s/div {:style {:margin-left (:spacing/padding theme)}}
      [s/div
       {:style {:margin (* -1 (:spacing/padding theme))}}
       [inspector value]]]]))

(defn- preview-coll [open close]
  (fn [value]
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

(defn- preview-object [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/text theme)}}
     (:type (.-rep value))]))

(defn preview-tagged [value]
  [tagged-tag (.-tag value)])

(defn- coll-action [props]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:border-right [1 :solid (::c/border theme)]}}
     [s/div
      {:on-click (:on-click props)
       :style/hover {:color (::c/tag theme)}
       :style {:cursor :pointer
               :user-select :none
               :color (::c/namespace theme)
               :box-sizing :border-box
               :padding (:spacing/padding theme)
               :font-size  (:font-size theme)
               :font-family (:font/family theme)}}
      (:title props)]]))

(defn- collection-header [values]
  (let [theme                       (theme/use-theme)
        [show-meta? set-show-meta!] (react/useState false)]
    [s/div
     {:style
      {:border [1 :solid (::c/border theme)]
       :background (get-background)
       :border-top-left-radius (:border-radius theme)
       :border-top-right-radius (:border-radius theme)
       :border-bottom-right-radius 0
       :border-bottom-left-radius 0
       :border-bottom :none}}
     [s/div
      {:style
       {:display :flex}}
      [s/div
       {:style
        {:display :inline-block
         :box-sizing :border-box
         :padding (:spacing/padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [preview values]]
      (when (meta values)
        [coll-action
         {:on-click
          (fn [e]
            (set-show-meta! not)
            (.stopPropagation e))
          :title "metadata"}])]
     (when show-meta?
       [s/div
        {:style
         {:border-top [1 :solid (::c/border theme)]
          :box-sizing :border-box
          :padding (:spacing/padding theme)}}
        [with-depth [inspector (meta values)]]])]))

(defn- container-map [values child]
  (let [theme (theme/use-theme)]
    [with-collection
     values
     [s/div
      [collection-header values]
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
         :border-bottom-left-radius (:border-radius theme)
         :border-bottom-right-radius (:border-radius theme)
         :border-top-right-radius 0
         :border-top-left-radius 0
         :border [1 :solid (::c/border theme)]}}
       child]]]))

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

(defn- inspect-map [values]
  [container-map
   values
   [l/lazy-seq
    (for [[k v] (try-sort-map values)]
      ^{:key {:key (hash k)}}
      [:<>
       [container-map-k [inspector k]]
       [with-key k
        [container-map-v [inspector v]]]])]])

(defn- container-coll [values child]
  (let [theme (theme/use-theme)]
    [with-collection
     values
     [s/div
      [collection-header values]
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
         :border-bottom-left-radius (:border-radius theme)
         :border-bottom-right-radius (:border-radius theme)
         :border [1 :solid (::c/border theme)]}}
       child]]]))

(defn- inspect-coll [values]
  [container-coll
   values
   [l/lazy-seq
    (map-indexed
     (fn [idx itm]
       ^{:key idx}
       [with-key idx [inspector itm]])
     values)]])

(defn- trim-string [string limit]
  (if-not (> (count string) limit)
    string
    (str (subs string 0 limit) "...")))

(defn- inspect-number [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}} value]))

(defn hex-color? [string]
  (re-matches #"#[0-9a-f]{6}|#[0-9a-f]{3}gi" string))

(defn- url-string? [string]
  (re-matches #"https?://.*" string))

(defn- inspect-string [value]
  (let [theme (theme/use-theme)
        limit (:limits/string-length theme)
        {:keys [expanded?]} @(state/use-state)
        context             (use-context)]
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

      (or (< (count value) limit)
          (= (:depth context) 1)
          (contains? expanded? context))
      [s/span {:style {:color (::c/string theme)}}
       (pr-str value)]

      :else
      [s/span {:style {:color (::c/string theme)}}
       (pr-str (trim-string value limit))])))

(defn- inspect-namespace [value]
  (let [theme (theme/use-theme)]
    (when-let [ns (namespace value)]
      [s/span
       [s/span {:style {:color (::c/namespace theme)}} ns]
       [s/span {:style {:color (::c/text theme)}} "/"]])))

(defn- inspect-boolean [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/boolean theme)}}
     (pr-str value)]))

(defn- inspect-symbol [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/symbol theme) :white-space :nowrap}}
     [inspect-namespace value]
     (name value)]))

(defn- inspect-keyword [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/keyword theme) :white-space :nowrap}}
     ":"
     [inspect-namespace value]
     (name value)]))

(defn- inspect-date [value]
  [tagged-value "inst" (.toJSON value)])

(defn- inspect-uuid [value]
  [tagged-value "uuid" (str value)])

(defn- get-var-symbol [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (let [m (meta value)]
                              (symbol (name (:ns m)) (name (:name m))))))

(defn- inspect-var [value]
  (let [theme (theme/use-theme)]
    [s/span
     [s/span {:style {:color (::c/tag theme)}} "#'"]
     [inspect-symbol (get-var-symbol value)]]))

(defn- get-url-string [value]
  (cond
    (t/tagged-value? value) (.-rep value)
    :else                   (str value)))

(defn- inspect-uri [value]
  (let [theme (theme/use-theme)
        value (get-url-string value)]
    [s/a
     {:href value
      :style {:color (::c/uri theme)}
      :target "_blank"}
     value]))

(defn- inspect-tagged [value]
  [tagged-value (.-tag value) (.-rep value)])

(defn- inspect-ratio [value]
  (let [theme (theme/use-theme)
        [a b] (.-rep value)]
    [s/div
     [s/span {:style {:color (::c/number theme)}} (.-rep a)]
     "/"
     [s/span {:style {:color (::c/number theme)}} (.-rep b)]]))

(defn- inspect-default [value]
  (let [theme (theme/use-theme)
        limit (:limits/string-length theme)]
    [s/span
     (trim-string (pr-str value) limit)]))

(defn- inspect-object [value]
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

(defn preview [value]
  (let [type (get-value-type value)
        component (get-preview-component type)]
    (when (preview-type? type)
      [component value])))

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

(defn inspector [value]
  (let [state (state/use-state)
        {:keys [selected expanded?]} @state
        context (-> (use-context)
                    (assoc :value value)
                    (update :depth inc))
        selected? (= selected context)]
    [with-context
     context
     (let [theme (theme/use-theme)
           depth (use-depth)
           preview? (and (not (contains? expanded? context))
                         (> depth (:limits/max-depth theme)))
           type (get-value-type value)
           component (if preview?
                       (get-preview-component type)
                       (get-inspect-component type))]
       [s/div
        {:tab-index 0
         :on-focus
         (fn [e]
           (when-not selected?
             (state/dispatch! state assoc :selected context))
           (.stopPropagation e))
         :on-mouse-down
         (fn [e]
           (when (= (.-button e) 1)
             (state/dispatch! state state/toggle-expand context)
             (.stopPropagation e)))
         :on-double-click
         (fn [e]
           (state/dispatch! state state/history-push {:portal/value value})
           (.stopPropagation e))
         :style {:width "100%"
                 :padding (when (or preview? (not (coll? value)))
                            (* 0.65 (:spacing/padding theme)))
                 :box-sizing :border-box
                 :border-radius (:border-radius theme)
                 :border (if selected?
                           [1 :solid "#D8DEE999"]
                           [1 :solid "rgba(0,0,0,0)"])
                 :background (when selected? "rgba(0,0,0,0.15)")}}
        [component value]])]))

(def viewer
  {:predicate (constantly true)
   :component inspector
   :name :portal.viewer/inspector})
