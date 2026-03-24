(ns portal.ssr.ui.inspector
  (:require
   [clojure.set :as set]
   [portal.colors :as c]
   [portal.runtime.cson :as cson]
   [portal.ssr.ui.icons :as icons]
   [portal.ssr.ui.lazy :as l]
   [portal.ssr.ui.react :as react]
   [portal.ui.select :as select]
   [portal.ssr.ui.state :as state]
   [portal.ssr.ui.styled :as s]
   [portal.ssr.ui.theme :as theme]
   [portal.ui.filter :as f]))

(declare inspector*)
(declare inspector)
(declare preview)

(defn regex? [value] (instance? java.util.regex.Pattern value))
(defn bigint? [value] (instance? clojure.lang.BigInt value))

(def viewer
  {:predicate (constantly true)
   :component #'inspector
   :name :portal.viewer/inspector})

(defonce viewers (atom [viewer]))

(defn viewers-by-name [viewers]
  (into {} (map (juxt :name identity) viewers)))

(defn- scalar? [value]
  (or (nil? value)
      (boolean? value)
      (number? value)
      (keyword? value)
      (symbol? value)
      (string? value)
      ;; (long? value)
      ;; (url? value)
      (bigint? value)
      (char? value)
      (ratio? value)
      (inst? value)
      (uuid? value)))

(defn- scalar-seq? [value]
  (and (coll? value)
       (seq value)
       (every? scalar? value)))

(defn- get-compatible-viewers-1 [viewers {:keys [value] :as context}]
  (let [by-name        (viewers-by-name viewers)
        default-viewer (get by-name
                            (or (get-in (meta context) [:props :portal.viewer/default])
                                (:portal.viewer/default (meta value))
                                (:portal.viewer/default context)
                                (when (scalar-seq? value)
                                  :portal.viewer/pprint)))
        viewers        (cons default-viewer (remove #(= default-viewer %) viewers))]
    (filter #(when-let [pred (:predicate %)] (pred value)) viewers)))

(defn get-compatible-viewers [viewers contexts]
  (if (nil? contexts)
    (get-compatible-viewers-1 viewers contexts)
    (->> contexts
         (map #(get-compatible-viewers-1 viewers %))
         (map set)
         (apply set/intersection))))

(defn- get-selected-viewer
  ([state context]
   (get-selected-viewer state context (:value context)))
  ([state context value]
   (get-selected-viewer state context (state/get-location context) value))
  ([state context location value]
   (when-let [selected-viewer
              (and (= (:value context) value)
                   (get-in @state [:selected-viewers location]))]
     (some #(when (= (:name %) selected-viewer) %) @viewers))))

(defn- get-compatible-viewer [context value]
  (first (get-compatible-viewers-1 @viewers (assoc context :value value))))

(defn get-viewer
  ([state context]
   (get-viewer state context (:value context)))
  ([state context value]
   (or (get-selected-viewer state context value)
       (get-compatible-viewer context value))))

(defn set-viewer-1 [state context viewer-name]
  (let [location (state/get-location context)]
    (assoc-in state [:selected-viewers location] viewer-name)))

(defn set-viewer [state contexts viewer-name]
  (reduce (fn [state context] (set-viewer-1 state context viewer-name)) state contexts))

(defn set-viewer! [state contexts viewer-name]
  (state/dispatch! state set-viewer contexts viewer-name))

(def ^:private parent-context (react/create-context nil))

(defn- use-parent [] (react/use-context parent-context))

(defn- with-parent [context & children]
  (apply react/provider parent-context context children))

(def ^:private inspector-context
  (react/create-context {:depth 0 :path [] :stable-path [] :alt-bg false}))

(defn use-context []
  (some->
   (react/use-context inspector-context)
   (vary-meta assoc ::select/position (select/use-position))))

(defn with-depth [& children]
  (let [context (use-context)]
    (apply react/provider inspector-context (assoc context :depth 0) children)))

(defn inc-depth [& children]
  (let [context (use-context)]
    (apply react/provider inspector-context (update context :depth inc) children)))

(defn dec-depth [& children]
  (let [context (use-context)]
    (apply react/provider inspector-context (update context :depth dec) children)))

(defn with-context [value & children]
  (let [context (use-context)]
    (apply react/provider inspector-context (merge context value) children)))

(defn with-default-viewer [viewer & children]
  (into [with-context {:portal.viewer/default viewer}] children))

(defn with-collection [coll & children]
  (into [with-context
         {:key nil
          :collection coll}]
        children))

(defn- get-stable-path
  "Since seqs grow at the front, reverse indexing them will yield a more stable
  path."
  [context k]
  (let [{:keys [collection stable-path] :or {stable-path []}} context]
    (if-not (and collection (seq? collection) (number? k))
      (conj stable-path k)
      (conj stable-path (- (count collection) k 1)))))

(defn with-key [k & children]
  (let [context (use-context)
        path    (get context :path [])]
    (into [with-context
           {:key         k
            :path        (conj path k)
            :stable-path (get-stable-path context k)}]
          children)))

(defn with-readonly [& children]
  (into [with-context {:readonly? true}] children))

(defonce ^:private options-context (react/create-context nil))

(defn use-options [] (react/use-context options-context))

(defn- with-options [options & children]
  (apply react/provider options-context options children))

(defn- get-value-type [value]
  (cond
    (tagged-literal? value)
    :tagged

    (bytes? value)    :binary
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
    ;; (error? value)    :error
    (char? value)     :char
    (ratio? value)    :ratio

    (uuid? value)     :uuid
    (uri? value)      :uri
    (inst? value)     :inst
    (regex? value)    :regex

    ;; (array? value)    :js-array
    ;; (object? value)   :js-object
    ))

;; (defn- child? [location context]
;;   (let [v   (:stable-path location)
;;         sub (:stable-path context)
;;         a (count v)
;;         b (count sub)]
;;     (cond
;;       (> a b) false
;;       :else   (= v (take a sub)))))

;; (defn- all-locations [state context]
;;   (let [search-text @(r/cursor state [:search-text])]
;;     (seq
;;      (reduce-kv
;;       (fn [out location search-text]
;;         (if-not (child? location context)
;;           out
;;           (into
;;            out
;;            (keep
;;             (fn [substring]
;;               (when-not (str/blank? substring)
;;                 {:substring substring
;;                  :context (-> location meta :context)}))
;;             (str/split search-text #"\s+")))))
;;       []
;;       search-text))))

;; (defn- use-search-words []
;;   (let [state   (state/use-state)
;;         context (use-context)]
;;     (when-not (:readonly? context)
;;       @(r/track all-locations state context))))

(defn toggle-bg [& children]
  (let [context (use-context)]
    (into [with-context (update context :alt-bg not)] children)))

(defn get-background
  ([]
   (get-background (use-context)))
  ([{:keys [alt-bg]}]
   (let [theme (theme/use-theme)]
     (if-not alt-bg
       (::c/background theme)
       (::c/background2 theme)))))

(defn get-background2 []
  (get-background (update (use-context) :alt-bg not)))

(defn- highlight-words* [string]
  string
  #_(let [theme        (theme/use-theme)
          state        (state/use-state)
          search-words (use-search-words)
          background   (get-background2)]
      (if-let [segments (some->> search-words (f/split string))]
        [l/lazy-seq
         (for [{:keys [context start end]} segments]
           ^{:key start}
           [s/span
            {:style
             (when context
               {:cursor :pointer
                :color background
                :background (get theme (nth theme/order (:depth context)))})
             :style/hover (when context {:text-decoration :underline})
             :on-click
             (fn [e]
               (when context
                 (.stopPropagation e)
                 (state/dispatch! state state/select-context context)))}
            (subs string start end)])
         {:default-take 5}]
        string)))

(defn highlight-words [string]
  (let [[sensor visible?] (l/use-visible)
        readonly?  (:readonly? (use-context))]
    (cond
      readonly? string
      visible?  [highlight-words* string]
      :else
      [s/span
       {:style {:position :relative}}
       string
       [s/div {:style {:position :absolute :top 2 :left 1}}
        sensor]])))

(defn- ->id [value]
  (str (hash value) (pr-str (type value))))

(defn tabs [value]
  (let [theme   (theme/use-theme)
        options (keys value)
        [option set-option!] (react/use-state (first options))
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
         ^{:key (->id value)}
         [s/div
          {:style
           {:flex "1"
            :cursor :pointer
            :border-right
            (if (= value (last options))
              :none
              [1 :solid (::c/border theme)])}
           :on-click
           (fn [_]
             (when-not (= value option)
               (set-option! value)))}
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
      [select/with-position
       {:row 0 :column 0}
       [dec-depth
        [with-key option [inspector (get value option)]]]]]]))

(defn- tagged-value [tag value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:position :relative :display :flex :gap (:padding theme)}}
     [s/div
      {:style {:position :sticky :top (:padding theme) :height :fit-content}}
      [s/div
       {:style {:display :flex
                :align-items :center}}
       [s/span {:style {:color (::c/tag theme)}} "#"]
       (when-let [ns (namespace tag)]
         [:<> [highlight-words ns] "/"])
       [highlight-words (name tag)]]]
     [s/div {:style
             {:flex "1"}}
      [with-key
       tag
       [select/with-position {:row 0 :column 0}
        [toggle-bg [inspector value]]]]]]))

(defn toggle-expand [{:keys [context style]}]
  (let [state     (state/use-state)
        context*  (use-context)
        context   (or context context*)
        theme     (theme/use-theme)
        color     (get theme (nth theme/order (:depth context)))
        {:keys [expanded?]} (use-options)]
    [s/div
     (merge
      {:title
       (if expanded?
         "Click to collapse value. - SPACE | E"
         "Click to expand value. - SPACE | E")
       :style
       (merge
        {:cursor :pointer
         :display :flex
         :align-items :center
         :color (::c/border theme)}
        style)
       :style/hover {:color color}
       :on-click (fn [e]
                   #_(set-expanded! not)
                   (if (:shift-key e)
                     (state/dispatch! state state/expand-inc-1 context)
                     (state/dispatch! state state/toggle-expand-1 context)))})
     (if expanded?
       [icons/caret-down]
       [icons/caret-right])]))

(defmulti inspect* #'get-value-type)

(defn- trim-string [string limit]
  (if-not (> (count string) limit)
    string
    (str (subs string 0 limit) "...")))

(defmethod inspect* :number [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}}
     (if-not (double? value)
       (str value)
       (cond
         (cson/nan? value)       "##NaN"
         (cson/inf? value)       "##Inf"
         (cson/-inf? value)      "##-Inf"
         :else                   (str value)))]))

(defmethod inspect* :bigint [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/number theme)}}
     [highlight-words (str value "N")]]))

(defmethod inspect* :char  [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/string theme)}}
     (pr-str value)]))

(defmethod inspect* :ratio [value]
  (let [theme (theme/use-theme)]
    [with-collection
     value
     [s/div
      {:style {:display :flex}}
      [with-key
       'numerator
       [select/with-position
        {:row 0 :column 0}
        [s/div [inspector (numerator value)]]]]
      [s/span {:style {:color (::c/number theme)}} "/"]
      [with-key
       'denominator
       [select/with-position
        {:row 0 :column 1}
        [s/div [inspector (denominator value)]]]]]]))

(defn- url-string? [string]
  (re-matches #"https?://.*" string))

(defmethod inspect* :string [value]
  (let [theme     (theme/use-theme)
        limit     (:string-length theme)
        context   (use-context)
        expanded? (:expanded? (use-options))]
    (cond
      (url-string? value)
      [s/span
       {:style {:color (::c/string theme)}}
       "\""
       [s/a
        {:href   value
         :target "_blank"
         :style  {:color (::c/string theme)}}
        (trim-string value limit)]
       "\""]

      (or (< (count value) limit)
          (= (:depth context) 1)
          expanded?)
      [s/span {:style {:white-space :pre-wrap :color (::c/string theme)}}
       (pr-str value)]

      :else
      [s/span {:style {:white-space :pre-wrap :color (::c/string theme)}}
       (pr-str (trim-string value limit))])))

(defn- inspect-namespace [value]
  (let [theme (theme/use-theme)]
    (when-let [ns (namespace value)]
      [s/span
       [s/span {:style {:color (::c/namespace theme)}}
        ns]
       [s/span {:style {:color (::c/text theme)}} "/"]])))

(defmethod inspect* :boolean [value]
  (let [theme (theme/use-theme)]
    [s/span
     {:style {:color (::c/boolean theme)}}
     (pr-str value)]))

(defmethod inspect* :symbol  [value]
  (let [theme (theme/use-theme)]
    [s/span {:style {:color (::c/symbol theme) :white-space :nowrap}}
     [inspect-namespace value]
     (name value)]))

(defmethod inspect* :keyword [value]
  (let [theme (theme/use-theme)]
    [s/span
     {:style
      {:color (::c/keyword theme)
       :font-size (:font-size theme)
       :font-family (:font-family theme)
       :white-space :nowrap}}
     ":"
     (when-not (:map-ns (:props (use-options)))
       [inspect-namespace value])
     (name value)]))

(defmethod inspect* :inst [value]
  [tagged-value
   'inst
   (let [date-str (pr-str value)]
     (subs date-str 7 (dec (count date-str))))])

(defmethod inspect* :uuid [value]
  [tagged-value 'uuid (str value)])

(defmethod inspect* :var  [value]
  (let [theme (theme/use-theme)]
    [s/span
     [s/span {:style {:color (::c/tag theme)}} "#'"]
     (inspect* (symbol value))]))

(defmethod inspect* :regex [value]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:display :flex}}
     [s/span {:style {:color (::c/tag theme)}} "#"]
     [select/with-position
      {:column 0 :row 0}
      [with-key :s [inspector (str value)]]]]))

(defmethod inspect* :uri [value]
  (let [theme (theme/use-theme)
        value (str value)]
    [s/a
     {:href value
      :style {:color (::c/uri theme)}
      :target "_blank"}
     value]))

(defmethod inspect* :tagged [value]
  [tagged-value (:tag value) (:form value)])

(defn- preview-coll* [value open close]
  (let [theme (theme/use-theme)]
    [s/div
     {:style {:display :flex
              :font-family (:font-family theme)}}
     [toggle-expand {:style {:padding-right (:padding theme)}}]
     [s/div
      {:style
       {:color (::c/diff-remove theme)}}
      open close [:sub (count value)]]]))

(defn preview-coll [value]
  (cond
    (map? value)    (preview-coll* value "{" "}")
    (vector? value) (preview-coll* value "[" "]")
    (set? value)    (preview-coll* value "#{" "}")
    :else           (preview-coll* value "(" ")")))

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

(defn use-search-text []
  (let [state       (state/use-state)
        context     (use-context)
        location    (state/get-location context)]
    (react/use-atom state #(get-in % [:search-text location]))))

(defn preview [value]
  (if-not (coll? value)
    (inspect* value)
    (preview-coll value)))

(defn- collection-header [values]
  (let [theme    (theme/use-theme)
        state    (state/use-state)
        context  (use-context)
        [show-meta? set-show-meta!] (react/use-state false)
        metadata (meta values)
        search-text (use-search-text)]
    [s/div
     {:style
      {:border [1 :solid (::c/border theme)]
       :background (get-background)
       :border-top-left-radius (:border-radius theme)
       :border-top-right-radius (:border-radius theme)
       :border-bottom-right-radius 0
       :border-bottom-left-radius 0
       :border-bottom :none
       :align-self :start}}
     [s/div
      {:style
       {:display :flex
        :align-items :stretch}}
      [s/div
       {:style
        {:display :inline-block
         :box-sizing :border-box
         :padding (:padding theme)
         :border-right [1 :solid (::c/border theme)]}}
       [preview values]]

      (when-let [ns (:map-ns (use-options))]
        [s/div {:title "Namespaced map."
                :style
                {:box-sizing :border-box
                 :padding (:padding theme)
                 :display :flex
                 :border-right [1 :solid (::c/border theme)]}}
         [s/span {:style {:color (::c/tag theme)}} "#"]
         [theme/with-theme+
          {::c/keyword (::c/namespace theme)}
          [select/with-position {:row -2 :column -1}
           [with-key 'ns [inspector ns]]]]])

      (when (seq metadata)
        [coll-action
         {:on-click
          (fn [_]
            (set-show-meta! not)
            #_(.stopPropagation e))
          :title "metadata"}])

      #_(when-let [type (-> values meta :portal.runtime/type)]
          [s/div {:title "Value type."
                  :style
                  {:box-sizing :border-box
                   :padding (:padding theme)
                   :border-right [1 :solid (::c/border theme)]}}
           [select/with-position {:row -2 :column 1} [inspector type]]])

      (when search-text
        (let [color (get theme (nth theme/order (:depth context)))]
          [s/div {:style
                  {:box-sizing :border-box
                   :display :flex
                   :align-items :center
                   :padding (:padding theme)
                   :gap  (:padding theme)
                   :color color
                   :width :fit-content
                   :border-right [1 :solid (::c/border theme)]}}
           [icons/filter {:size :sm}]
           [s/div
            {:title "Filter text"
             :style {:background :none
                     :color color
                     :font-family (:font-family theme)}}
            search-text]
           [icons/times-circle
            {:size :sm
             :title "Clear filter text"
             :style {:color (::c/border theme)
                     :cursor :pointer}
             :style/hover {:color (::c/diff-remove theme)}
             :on-click (fn [_]
                         (state/dispatch! state state/clear-search context))}]]))]

     (when show-meta?
       [s/div
        {:style
         {:border-top [1 :solid (::c/border theme)]
          :box-sizing :border-box
          :padding (:padding theme)}}
        [with-depth
         [select/with-position {:row -1 :column 0} [inspector metadata]]]])]))

(defn- container-map-k [child]
  (let [theme (theme/use-theme)]
    [s/div {:style
            {:grid-column "1"
             :display :flex
             :align-items :flex-start}}
     [s/div {:style
             {:width "100%"
              :top (:padding theme)
              :position :sticky}}
      child]]))

(defn- container-map-v [child]
  (let [theme (theme/use-theme)]
    [s/div {:style
            {:grid-column "2"
             :display :flex
             :align-items :flex-start}}
     [s/div {:style
             {:width "100%"
              :top (:padding theme)
              :position :sticky}}
      child]]))

(defn try-sort [values]
  (if (sorted? values)
    values
    (try (sort values)
         (catch Exception _e values))))

(defn try-sort-map [values]
  (if (sorted? values)
    values
    (try (sort-by first values)
         (catch Exception _e values))))

;; (defn use-sort-map [values]
;;   (react/use-memo #js [values] (try-sort-map values)))

(defn- container-map [child]
  (let [theme (theme/use-theme)]
    [s/div
     {:style
      {:width "100%"
       :display :grid
       :grid-template-columns "auto 1fr"
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

(defn get-props [value k]
  (let [m (meta value)]
    (when-let [viewer (get-in m [:portal.viewer/for k])]
      (merge
       (select-keys m [viewer])
       {:portal.viewer/default viewer}))))

(defn- inspect-map-k-v* [map-ns search-text values]
  (let [matcher       (f/match search-text)
        ;sorted-values (use-sort-map values)
        sorted-values values]
    ^{:key search-text}
    [container-map
     [l/lazy-seq
      (keep-indexed
       (fn [index [k v]]
         (when (or (matcher k) (matcher v))
           ^{:key (str (->id k) (->id v))}
           [with-key k
            [select/with-position
             {:row index :column 0}
             [with-context
              {:key? true}
              [container-map-k
               [with-key ::key
                [inspector
                 {:map-ns map-ns}
                 k]]]]]
            [select/with-position
             {:row index :column 1}
             [container-map-v
              [inspector (get-props values k) v]]]]))
       sorted-values)]]))

(defn inspect-map-k-v [values]
  (let [map-ns (:map-ns (use-options))]
    [inspect-map-k-v* map-ns (use-search-text) values]))

(defn- get-namespaces [value]
  (when (map? value)
    (into #{}
          (map (fn [k]
                 (when (keyword? k)
                   (some-> (namespace k) keyword))))
          (keys value))))

(defn- namespaced-map [value]
  (let [namespaces (get-namespaces value)]
    (when (= 1 (count namespaces)) (first namespaces))))

(defmethod inspect* :map [values]
  (let [map-ns  (namespaced-map values)
        options (use-options)]
    [with-options
     (assoc options :map-ns map-ns)
     [with-collection
      values
      [:<>
       ^{:key "header"} [collection-header values]
       ^{:key "values"} [inspect-map-k-v values]]]]))

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

(defn- inspect-coll* [search-text values]
  (let [n (count values)
        matcher     (f/match search-text)]
    ^{:key search-text}
    [container-coll
     values
     [l/lazy-seq
      (keep-indexed
       (fn [index value]
         (when (matcher value)
           (let [key (str (if (vector? values)
                            index
                            (- n index 1))
                          (->id value))]
             ^{:key key}
             [select/with-position
              {:row index :column 0}
              [with-key index [inspector value]]])))
       values)]]))

(defmethod inspect* :set [value]
  [inspect-coll* (use-search-text) value])

(defmethod inspect* :vector [value]
  [inspect-coll* (use-search-text) value])

(defmethod inspect* :list [value]
  [inspect-coll* (use-search-text) value])

(defmethod inspect* :coll [value]
  [inspect-coll* (use-search-text) value])

(defmethod inspect* :default [value] [s/span (pr-str value)])

(defn- default-expand? [state theme context value]
  (let [depth   (:depth context)
        viewer  (get-viewer state context value)]
    (or (= depth 1)
        (= (:name viewer) :portal.viewer/tree)
        (and (coll? value)
             (= (:name viewer) :portal.viewer/inspector)
             (<= depth (:max-depth theme))))))

(defn- get-info [state context location value]
  (let [state (atom state)]
    {:expanded? (state/expanded? state context)
     :selected  (state/selected state context)
     :viewer    (or (get-selected-viewer state context location value)
                    (get-compatible-viewer context value))}))

(defn use-wrapper-options [context]
  (let [state          (state/use-state)
        ;; location       (state/get-location context)
        {:keys [viewer selected]} (use-options)]
    (when-not (:readonly? context)
      {:on-mouse-down
       (fn [_]
         #_(.stopPropagation e)
         #_(when (= (.-button e) 1)
             (state/dispatch! state state/toggle-expand location)))
       :on-click
       (fn [e]
         (set-viewer! state [context] (:name viewer))
         (state/dispatch!
          state
          (if selected
            state/deselect-context
            state/select-context)
          context
          (or (:meta-key e) (:alt-key e))))
       :on-double-click
       (fn [_]
         (set-viewer! state [context] (:name viewer))
         (state/dispatch! state state/select-context context)
         (state/dispatch! state state/nav context))})))

(defn- multi-select-counter [context]
  (let [theme      (theme/use-theme)
        selected   (:selected (use-options))
        state      (state/use-state)
        background (get-background)
        multi?     (react/use-atom state #(> (count (get-in % [:selected])) 1))]
    (when (and selected multi?)
      [s/div {:style
              {:position :absolute
               :font-size "0.8rem"
               :color background
               :border-radius 100
               :top "-0.4rem"
               :right "-0.4rem"
               :min-width "1rem"
               :height "1rem"
               :z-index 3
               :display :flex
               :align-items :center
               :justify-content :center
               :background (get theme (nth theme/order (:depth context)))}}
       selected])))

(defn- inspector-border [context]
  (let [theme    (theme/use-theme)
        selected (:selected (use-options))
        color    (get theme (nth theme/order (:depth context)))
        transition "all 0.35s ease-in-out"]
    [:<>
     [s/div
      {:style
       (merge
        {:position :absolute
         :pointer-events :none
         :top 0
         :left 0
         :right 0
         :bottom 0
         :z-index 2
         :transition transition
         :border [1 :solid "rgba(0,0,0,0)"]
         :border-radius (:border-radius theme)}
        (when selected
          {:border [1 :solid color]
           :box-shadow [0 0 5 color]}))
       :style/parent-hover
       {:border-top-left-radius 0
        :border-bottom-left-radius 0}}]
     [s/div
      {:style
       (merge
        {:position :absolute
         :pointer-events :none
         :z-index 2
         :left (- (dec (:padding theme)))
         :top 0
         :bottom 0
         :border-radius (:border-radius theme)
         :transition transition
         :opacity 0
         :border-left [(- (:padding theme) 1) :solid color]}
        (when selected {:right 0}))
       :style/parent-hover
       {:opacity 1}}]]))

(defn wrapper [context & children]
  (let [theme          (theme/use-theme)
        {:keys [value selected]} (use-options)
        wrapper-options (use-wrapper-options context)
        background (get-background context)]
    (into
     (if (:readonly? context)
       [s/div
        {:title (-> value meta :doc)}]
       [s/div
        (merge
         wrapper-options
         {;:ref   ref
          :class "parent"
          :title (-> value meta :doc)
          #_#_:on-mouse-over
            (fn [e]
              (.stopPropagation e)
              (reset! hover? context))
          :style
          {:position      :relative
           :z-index       0
           :flex          "1"
           :font-family   (:font-family theme)
           :border        [1 :solid "rgba(0,0,0,0)"]
           :background    (when selected background)}})
        ^{:key "inspector-border"} [inspector-border context]
        ^{:key "multi-select-counter"} [multi-select-counter context]])
     children)))

(defn inspect [value] (inspect* value))

(defn inspector* [ctx value]
  (let [props          (:props (meta ctx))
        state          (state/use-state)
        location       (state/get-location ctx)
        theme          (theme/use-theme)
        {:keys [viewer expanded?] :as options}
        (react/use-atom state #(get-info % ctx location value))

        options        (assoc options :props props)
        component (or
                   (when-not (= (:name viewer) :portal.viewer/inspector)
                     @(:component viewer))
                   (if (and (not expanded?) (coll? value))
                     preview-coll
                     inspect))]
    (select/use-register-context ctx viewer)
    (react/use-effect
     (fn []
       (when (and (nil? expanded?)
                  (default-expand? state theme ctx value))
         (state/dispatch!
          state assoc-in [:expanded? location]
          (get-in (meta value) [:portal.viewer/inspector :expanded] 1))))
     [location (some? expanded?)])
    [with-options options
     [(get-in props [:portal.viewer/inspector :wrapper] wrapper)
      ctx [component value]]]))

(defn- tab-index [context]
  (let [;; ref      (react/use-ref)
        state    (state/use-state)
        selected (state/selected state context)]
    ;; (react/use-effect
    ;;  #js [selected (.-current ref)]
    ;;  (when (and selected (.hasFocus js/document))
    ;;    (some-> ref .-current (.focus #js {:preventScroll true}))))
    (when-not (:readonly? context)
      [s/div
       {;:ref         ref
        :tabindex   0
        :style/focus {:outline :none}
        :style       {:position :absolute}
        :on-focus
        (fn [_]
          (when-not selected
            (state/dispatch! state state/select-context context false))
          #_(when-not selected (set-selected! [id])))}])))

(defn inspector
  ([value]
   (inspector nil value))
  ([props value]
   (let [context
         (cond->
          (-> (use-context)
              (assoc :value value)
              (update :depth inc)
              (assoc :parent (use-parent)))
           (get-in props [:portal.viewer/inspector :toggle-bg] true)
           (update :alt-bg not)
           props
           (vary-meta assoc :props props)
           (nil? props)
           (vary-meta dissoc :props props))]
     [with-parent
      context
      ^{:key "tab-index"} [tab-index context]
      [with-context context ^{:key value} [inspector* context value]]])))
