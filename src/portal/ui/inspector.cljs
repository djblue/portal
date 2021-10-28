(ns portal.ui.inspector
  (:require ["react" :as react]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.colors :as c]
            [portal.runtime.cson :as cson]
            [portal.ui.filter :as f]
            [portal.ui.lazy :as l]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(defn- inspect-error [error]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:color         (::c/exception theme)
       :border        [1 :solid (::c/exception theme)]
       :background    (str (::c/exception theme) "22")
       :border-radius (:border-radius theme)}}
     [s/div
      {:style
       {:padding       (:padding theme)
        :border-bottom [1 :solid (::c/exception theme)]}}
      "Rendering error: "]
     [s/div
      {:style
       {:padding (:padding theme)}}
      [:pre
       [:code (.-stack error)]]]]))

(defn- inspect-error* [error] [:f> inspect-error error])

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
        (r/as-element [inspect-error* error])
        (.. this -props -children)))}))

(defonce viewers (atom []))

(defn viewers-by-name [viewers]
  (into {} (map (juxt :name identity) viewers)))

(defn get-compatible-viewers [viewers value]
  (let [by-name        (viewers-by-name viewers)
        default-viewer (get by-name (:portal.viewer/default (meta value)))
        viewers        (cons default-viewer (remove #(= default-viewer %) viewers))]
    (filter #(when-let [pred (:predicate %)] (pred value)) viewers)))

(defn get-viewer [state context]
  (if-let [selected-viewer
           (get-in @state [:selected-viewers (state/get-location context)])]
    (some #(when (= (:name %) selected-viewer) %) @viewers)
    (first (get-compatible-viewers @viewers (:value context)))))

(defn set-viewer! [state context viewer-name]
  (state/dispatch! state
                   assoc-in [:selected-viewers (state/get-location context)]
                   viewer-name))

(def ^:private inspector-context (react/createContext {:depth 0 :path []}))

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
  (into [with-context
         {:key nil
          :collection (:portal.ui.filter/value (meta coll) coll)}]
        children))

(defn with-key [k & children]
  (let [context (use-context)
        path    (get context :path [])]
    (into [with-context {:key k :path (conj path k)}] children)))

(defn with-readonly [& children]
  (into [with-context {:readonly? true}] children))

(defn date? [value] (instance? js/Date value))
(defn url? [value] (instance? js/URL value))
(defn bin? [value] (instance? js/Uint8Array value))
(defn bigint? [value] (= (type value) js/BigInt))

(defn get-value-type [value]
  (cond
    (cson/tagged-value? value)
    (:tag value)

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
    (bigint? value)   :bigint
    (number? value)   :number
    (string? value)   :string
    (keyword? value)  :keyword
    (var? value)      :var

    (uuid? value)     :uuid
    (url? value)      :uri
    (date? value)     :date

    (rpc/runtime-object? value)
    (rpc/tag value)))

(declare inspector)
(declare preview)

(defn get-background []
  (let [theme (theme/use-theme)]
    (if (even? (use-depth))
      (::c/background theme)
      (::c/background2 theme))))

(defn tabs [value]
  (let [theme   (theme/use-theme)
        options (keys value)
        [option set-option!] (react/useState (first options))
        background (get-background)]
    [s/div
     {:style
      {:background background
       :border [1 :solid (::c/border theme)]
       :border-radius (:border-radius theme)}}
     [with-readonly
      [s/div
       {:style
        {:display :flex
         :align-items :stretch
         :border-bottom [1 :solid (::c/border theme)]}}
       (for [value options]
         ^{:key (hash value)}
         [s/div
          {:style
           {:flex "1"
            :cursor :pointer
            :border-right
            (if (= value (last options))
              :none
              [1 :solid (::c/border theme)])}
           :on-click
           (fn [e]
             (set-option! value)
             (.stopPropagation e))}
          [s/div
           {:style {:box-sizing :border-box
                    :padding (:padding theme)
                    :border-bottom
                    (if (= value option)
                      [5 :solid (::c/boolean theme)]
                      [5 :solid (::c/border theme)])}}
           [preview value]]])]]
     [s/div
      {:style
       {:box-sizing :border-box
        :padding (:padding theme)}}
      [with-key option [inspector (get value option)]]]]))

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
                  (:padding theme))}}
        [diff-removed removed]])
     (when-not (= added ::not-found)
       [s/div {:style {:flex 1}}
        [diff-added added]])]))

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
     [s/div {:style {:margin-left (:padding theme)}}
      [select/with-position {:row 0 :column 0} [inspector value]]]]))

(defn- preview-coll [open close]
  (fn [value]
    (let [theme (theme/use-theme)]
      [s/div
       {:style
        {:color (::c/diff-remove theme)}}
       open
       (count value)
       close])))

(def ^:private preview-map    (preview-coll "{" "}"))
(def ^:private preview-vector (preview-coll "[" "]"))
(def ^:private preview-list   (preview-coll "(" ")"))
(def ^:private preview-set    (preview-coll "#{" "}"))

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
               :padding (:padding theme)
               :font-size  (:font-size theme)
               :font-family (:font-family theme)}}
      (:title props)]]))

(defn- collection-header [values]
  (let [[show-meta? set-show-meta!] (react/useState false)
        theme    (theme/use-theme)
        metadata (dissoc
                  (meta values)
                  :portal.runtime/id
                  :portal.runtime/type)]
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
       {:display :flex
        :align-items :center}}
      [s/div
       {:style
        {:display :inline-block
         :box-sizing :border-box
         :padding (:padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [preview values]]
      (when (seq metadata)
        [coll-action
         {:on-click
          (fn [e]
            (set-show-meta! not)
            (.stopPropagation e))
          :title "metadata"}])

      (when-let [type (-> values meta :portal.runtime/type)]
        [s/div {:style
                {:box-sizing :border-box
                 :padding (:padding theme)
                 :border-right [1 :solid (::c/border theme)]}}
         [inspector type]])]
     (when show-meta?
       [s/div
        {:style
         {:border-top [1 :solid (::c/border theme)]
          :box-sizing :border-box
          :padding (:padding theme)}}
        [with-depth [inspector metadata]]])]))

(defn- container-map-k [child]
  [s/div {:style
          {:grid-column "1"
           :display :flex
           :align-items :flex-start}}
   [s/div {:style
           {:width "100%"
            :top 0
            :position :sticky}}
    child]])

(defn- container-map-v [child]
  [s/div {:style
          {:grid-column "2"
           :display :flex
           :align-items :flex-start}}
   [s/div {:style
           {:width "100%"
            :top 0
            :position :sticky}}
    child]])

(defn try-sort [values]
  (try (sort values)
       (catch :default _e values)))

(defn try-sort-map [values]
  (try (sort-by first values)
       (catch :default _e values)))

(defn- container-map [child]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:width "100%"
       :min-width :fit-content
       :display :grid
       :background (get-background)
       :grid-gap (:padding theme)
       :padding (:padding theme)
       :box-sizing :border-box
       :color (::c/text theme)
       :font-size  (:font-size theme)
       :border-bottom-left-radius (:border-radius theme)
       :border-bottom-right-radius (:border-radius theme)
       :border-top-right-radius 0
       :border-top-left-radius 0
       :border [1 :solid (::c/border theme)]}}
     child]))

(defn- inspect-map-k-v [values]
  [container-map
   [l/lazy-seq
    (map-indexed
     (fn [index [k v]]
       ^{:key (hash k)}
       [:<>
        [select/with-position
         {:row index :column 0}
         [with-context
          {:key? true}
          [container-map-k [inspector k]]]]
        [select/with-position
         {:row index :column 1}
         [with-key k
          [container-map-v [inspector v]]]]])
     (try-sort-map values))]])

(defn- inspect-map [values]
  [with-collection
   values
   [:<>
    [collection-header values]
    [inspect-map-k-v values]]])

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
         :grid-gap (:padding theme)
         :padding (:padding theme)
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
     (fn [index value]
       ^{:key index}
       [select/with-position
        {:row index :column 0}
        [with-key index [inspector value]]])
     values)]])

(defn- trim-string [string limit]
  (if-not (> (count string) limit)
    string
    (str (subs string 0 limit) "...")))

(defn- inspect-number [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}} value]))

(defn- inspect-bigint [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}} (str value) "N"]))

(defn hex-color? [string]
  (re-matches #"#[0-9a-fA-F]{6}|#[0-9a-fA-F]{3}gi" string))

(defn- url-string? [string]
  (re-matches #"https?://.*" string))

(defn- inspect-string [value]
  (let [theme (theme/use-theme)
        limit (:string-length theme)
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
        {:padding (* 0.65 (:padding theme))
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
          (contains? expanded? (state/get-location context)))
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
  (if (rpc/runtime-object? value)
    (rpc/rep value)
    (let [m (meta value)]
      (symbol (name (:ns m)) (name (:name m))))))

(defn- inspect-var [value]
  (let [theme (theme/use-theme)]
    [s/span
     [s/span {:style {:color (::c/tag theme)}} "#'"]
     [inspect-symbol (get-var-symbol value)]]))

(defn- inspect-uri [value]
  (let [theme (theme/use-theme)
        value (str value)]
    [s/a
     {:href value
      :style {:color (::c/uri theme)}
      :target "_blank"}
     value]))

(defn- inspect-tagged [value]
  [tagged-value (.-tag value) (.-rep value)])

(defn- inspect-object [value]
  (let [theme  (theme/use-theme)
        string (rpc/use-invoke 'clojure.core/pr-str value)
        limit  (:string-length theme)
        {:keys [expanded?]} @(state/use-state)
        context             (use-context)]
    (when-not (= string ::rpc/loading)
      [s/span {:style
               {:color (::c/text theme)}}
       (if (or (< (count string) limit)
               (= (:depth context) 1)
               (contains? expanded? (state/get-location context)))
         string
         (trim-string string limit))])))

(defn inspect-long [value]
  [inspect-number (:rep value)])

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
    :bigint     inspect-bigint
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :uri        inspect-uri
    :tagged     preview-tagged
    "long"      inspect-long
    inspect-object))

(defn preview [value]
  (let [type      (get-value-type value)
        component (get-preview-component type)]
    [component value]))

(defn- get-inspect-component [type]
  (case type
    :diff       inspect-diff
    (:set :vector :list :coll) inspect-coll
    :map        inspect-map
    :boolean    inspect-boolean
    :symbol     inspect-symbol
    :number     inspect-number
    :bigint     inspect-bigint
    :string     inspect-string
    :keyword    inspect-keyword
    :date       inspect-date
    :uuid       inspect-uuid
    :var        inspect-var
    :uri        inspect-uri
    :tagged     inspect-tagged
    "long"      inspect-long
    inspect-object))

(defn get-info [state context]
  (let [{:keys [search-text selected expanded?]} @state
        location (state/get-location context)]
    {:selected? (= selected context)
     :expanded? (contains? expanded? location)
     :viewer    (get-viewer state context)
     :value     (f/filter-value (:value context)
                                (get search-text location ""))}))

(def ^:private selected-ref (atom nil))

(defn focus-selected []
  (some-> selected-ref deref .-current .focus))

(defn inspector [value]
  (let [ref   (react/useRef nil)
        state (state/use-state)
        context
        (-> (use-context)
            (assoc :value value)
            (update :depth inc))
        location (state/get-location context)
        {:keys [value viewer selected? expanded?]}
        @(r/track get-info state context)]
    (select/use-register-context context viewer)
    [with-context
     context
     (let [theme (theme/use-theme)
           depth (use-depth)
           preview? (and (not expanded?)
                         (> depth (:max-depth theme)))
           type (get-value-type value)
           component (or
                      (when-not (= (:name viewer) :portal.viewer/inspector)
                        (:component viewer))
                      (if preview?
                        (get-preview-component type)
                        (get-inspect-component type)))]
       (react/useEffect
        (fn []
          (when (and selected?
                     (not= (.. js/document -activeElement -tagName) "INPUT"))
            (reset! selected-ref ref)
            (some-> ref .-current .focus)))
        #js [selected? (.-current ref)])
       [s/div
        (merge
         (when-not (:readonly? context)
           {:tab-index 0
            :on-focus
            (fn [e]
              (when-not selected?
                (state/dispatch! state assoc :selected context))
              (.stopPropagation e))
            :on-mouse-down
            (fn [e]
              (when (= (.-button e) 1)
                (state/dispatch! state state/toggle-expand location)
                (.stopPropagation e)))
            :on-click
            (fn [e]
              (.stopPropagation e))
            :on-double-click
            (fn [e]
              (state/dispatch! state state/nav context)
              (.stopPropagation e))})
         {:ref ref
          :title (-> value meta :doc)
          :style
          {:width "100%"
           :border-radius (:border-radius theme)
           :border (if selected?
                     [1 :solid "#D8DEE999"]
                     [1 :solid "rgba(0,0,0,0)"])
           :background (when selected? "rgba(0,0,0,0.15)")}})
        [:> error-boundary [component value]]])]))

(def viewer
  {:predicate (constantly true)
   :component inspector
   :name :portal.viewer/inspector})
