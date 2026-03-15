(ns portal.ssr.ui.inspector
  (:require
   [clojure.data.json :as json]
   [clojure.string :as str]
   [portal.colors :as c]
   [portal.runtime :as rt]
   [portal.runtime.render :as r]))

(def context (r/create-context {:depth 0 :path [] :stable-path [] :alt-bg false}))

(defn use-context [] (r/use-context context))

(defn use-depth [] (:depth (use-context)))

(def ^:dynamic *theme* (merge
                        (::c/nord c/themes)
                        {:font-family   "monospace"
                         :font-size     "12pt"
                         :string-length 100
                         :max-depth     2
                         :padding       6
                         :border-radius 2}))

(defn get-background
  ([]
   (get-background (use-context)))
  ([{:keys [alt-bg]}]
   (if-not alt-bg
     (::c/background *theme*)
     (::c/background2 *theme*))))

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
    ;; (bigint? value)   :bigint
    (number? value)   :number
    (string? value)   :string
    (keyword? value)  :keyword
    (var? value)      :var
    ;; (error? value)    :error
    (char? value)     :char
    (ratio? value)    :ratio

    (uuid? value)     :uuid
    ;; (url? value)      :uri
    (inst? value)     :inst

    ;; (array? value)    :js-array
    ;; (object? value)   :js-object
    ))

(defmulti inspect* #'get-value-type)

(declare inspect)

(defn- trim-string [string limit]
  (if-not (> (count string) limit)
    string
    (str (subs string 0 limit) "...")))

(defmethod inspect* :string [value]
  (let [limit (:string-length *theme* 25)]
    (if (<= (count value) limit)
      [:span {:style {:white-space :pre-wrap :color (::c/string *theme*)}}
       (pr-str value)]
      [:span {:style {:white-space :pre-wrap :color (::c/string *theme*)}}
       (pr-str (trim-string value limit))])))

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

(defn- coll-action [props]
  (let [theme *theme*]
    [:div
     {:style {:border-right [1 :solid (::c/border theme)]}}
     [:div
      {;:on-click (:on-click props)
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
  (let [;;[show-meta? set-show-meta!] (react/use-state false)
        ;; theme    (theme/use-theme)
        ;; state    (state/use-state)
        ;; context  (use-context)
        metadata (meta values)
        show-meta? (and true (not-empty metadata))
        ;; search-text (use-search-text)
        ]
    [:div
     {:style
      {:border [1 :solid (::c/border *theme*)]
       :background (get-background)
       :border-top-left-radius (:border-radius *theme*)
       :border-top-right-radius (:border-radius *theme*)
       :border-bottom-right-radius 0
       :border-bottom-left-radius 0
       :border-bottom :none
       :align-self :start}}
     [:div
      {:style
       {:display :flex
        :align-items :stretch}}
      [:div
       {:style
        {:display :inline-block
         :box-sizing :border-box
         :padding (:padding *theme*)
         :border-right [1 :solid (::c/border *theme*)]}}
       #_[preview values]]

      #_(when-let [ns (:map-ns (use-options))]
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
         {#_#_:on-click
            (fn [e]
              (prn e)
              #_(set-show-meta! not)
              #_(.stopPropagation e))
          :title "metadata"}])

      #_(when-let [type (-> values meta :portal.runtime/type)]
          [s/div {:title "Value type."
                  :style
                  {:box-sizing :border-box
                   :padding (:padding theme)
                   :border-right [1 :solid (::c/border theme)]}}
           [select/with-position {:row -2 :column 1} [inspector type]]])

      #_(when search-text
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
               :on-click (fn [e]
                           (.stopPropagation e)
                           (state/dispatch! state state/clear-search context))}]]))]

     (when show-meta?
       [:div
        {:style
         {:border-top [1 :solid (::c/border *theme*)]
          :box-sizing :border-box
          :padding (:padding *theme*)}}
        [inspect metadata]])]))

(defn- container-map [child]
  [:div
   {:style
    {:width "100%"
     :display :grid
     :grid-template-columns "auto 1fr"
     :background (get-background)
     :grid-gap (:padding *theme*)
     :padding (:padding *theme*)
     :box-sizing :border-box
     :color (::c/text *theme*)
     :font-size  (:font-size *theme*)
     :border-bottom-left-radius (:border-radius *theme*)
     :border-bottom-right-radius (:border-radius *theme*)
     :border-top-right-radius 0
     :border-top-left-radius 0
     :border [1 :solid (::c/border *theme*)]}}
   child])

(defn- container-map-k [child]
  [:div {:style
         {:grid-column "1"
          :display :flex
          :align-items :flex-start}}
   [:div {:style
          {:width "100%"
           :top (:padding *theme*)
           :position :sticky}}
    child]])

(defn- container-map-v [child]
  [:div {:style
         {:grid-column "2"
          :display :flex
          :align-items :flex-start}}
   [:div {:style
          {:width "100%"
           :top (:padding *theme*)
           :position :sticky}}
    child]])

(defn- inspect-map-k-v [map-ns search-text value]
  (let [;matcher       (f/match search-text)
        ;sorted-values (use-sort-map values)
        ]
    [container-map
     (map-indexed
      (fn [index [k v]]
        ^{:key k :row index :column 0}
        [:<>
         [container-map-k [inspect k]]
         [container-map-v ^{:row index :column 1} [inspect v]]])
      value)]))

(defmethod inspect* :map [value]
  (let [map-ns  (namespaced-map value)]
    [:<>
     [collection-header value]
     [inspect-map-k-v nil nil value]]))

(defn- inspect-namespace [value]
  (let [theme *theme*]
    (when-let [ns (namespace value)]
      [:span
       [:span {:style {:color (::c/namespace theme)}}
        ns]
       [:span {:style {:color (::c/text theme)}} "/"]])))

(defmethod inspect* :keyword [value]
  [:span
   {:style
    {:color (::c/keyword *theme*)
     :font-size (:font-size *theme*)
     :font-family (:font-family *theme*)
     :white-space :nowrap}}
   ":"
   [inspect-namespace value]
   (name value)])

(defmethod inspect* :boolean [value]
  [:span
   {:style {:color (::c/boolean *theme*)}}
   (pr-str value)])

(defmethod inspect* :default [value] [:span (pr-str value)])

(defn inspect [value]
  (let [ctx (use-context)]
    [(:provider context)
     {:value (-> ctx
                 (update :depth inc)
                 (update :alt-bg not))}
     (inspect* value)]))

(require '[portal.ssr.ui.css :as css])

(defn ->attrs [m]
  (reduce-kv
   (fn [out k v]
     (str out " " (name k) "=" v))
   ""
   m))

(def ^:dynamic *handler* nil)

(defn on-click-handler [{:keys [on-click] :as attrs}]
  (let [id (random-uuid)]
    (swap! *handler* assoc-in [id :on-click] on-click)
    (-> attrs
        (dissoc :on-click)
        (assoc :data-on-click (str id)))))

(defn- extract-handlers [attrs]
  (cond-> attrs
    (:on-click attrs) on-click-handler))

(defn html [hiccup]
  (cond
    (or (list? hiccup) (seq? hiccup))
    (str/join "" (map html hiccup))

    (vector? hiccup)
    (let [[tag & args] hiccup
          attrs (when (map? (first args)) (first args))
          children (cond-> args (map? (first args)) rest)]
      (if (= :<> tag)
        (str/join "" (map html children))
        (str "<"
             (name tag)
             (when attrs (-> attrs
                             extract-handlers
                             css/attrs->css
                             ->attrs))
             ">"
             (str/join "" (map html children))
             "</" (name tag) ">")))

    :else hiccup))

(defmulti on-message* :op)

(def handlers (atom {}))

(defmethod on-message* "on-click" [{:keys [id] :as event}]
  (when-let [f (get-in @handlers [(parse-uuid id) :on-click])]
    (f event)))

(defmethod on-message* :default [event] (prn event))

(defn on-message [data]
  (on-message* (json/read-str data :key-fn keyword)))

(defn start-render-loop [render]
  (let [running (atom true)]
    (future
      (let [budget-time (/ 1000.0 30)]
        (loop [state nil]
          (when @running
            (recur
             (let [start (System/currentTimeMillis)]
               (try
                 (render state)
                 (catch Exception e
                   (reset! running false)
                   (println e))
                 (finally
                   (let [total-time (- (System/currentTimeMillis) start)]
                     (when (< total-time budget-time)
                       (Thread/sleep (long (- budget-time total-time)))))))))))))
    (fn stop-render-loop []
      (reset! running false))))

(defonce render-loops (atom {}))

(count @render-loops)

(defn ->style [cache]
  (str
   "<style>"
   (reduce-kv
    (fn [out [selector style] class]
      (str out
           ((get css/selectors selector) class)
           "{" (css/style->css style) "}"
           "\n"))
    ""
    cache)
   "</style>"))

(def cache (atom {}))

(defn render-app [session state]
  (reset! handlers {})
  (let [hiccup
        (r/render
         [:div {:style
                {:padding 20
                 :font-size (:font-size *theme*)
                 :font-family (:font-family *theme*)}}
          [inspect (-> session :options :value deref)]])]
    (when-not (= hiccup state)
      ((get @rt/connections (:session-id session))
       (binding [css/*cache* cache
                 *handler* handlers]
         (let [h (html hiccup)]
           (str (->style @cache) h)))))
    hiccup))

(defn on-open [session]
  (swap! render-loops assoc (:session-id session)
         (start-render-loop (partial #'render-app session))))

(defn on-close [session]
  (when-let [stop-render-loop (get @render-loops (:session-id session))]
    (stop-render-loop)
    (swap! render-loops dissoc (:session-id session))))

(comment
  (require '[examples.data :as data])
  (require '[portal.api :as p])

  (def a (atom {}))
  (reset! a ^:foo {:hello :world :foo :bar})
  (swap! a empty)

  (def ssr (p/open {:mode :ssr
                    :value a
                    :on-message #'on-message
                    :on-open #'on-open
                    :on-close #'on-close}))

  ;; rebuild inspector context

  ;; re-render at 60 fps?
  ;; don't care about specific state, just capture consistent view



  ;; hoist up state and make explicit to check if we need to re-render?

  comment)

;; portal ui features
;; [ ] select values
;; [ ] value filtering
;; [ ] relative selection via keyboard
;; [ ] expand collapse values
;; [ ] pause / resume watching values
;; [ ] hover preview
;; [ ] path tracking

;; architecture
;; single state value
;; single render function
;; state updates are queued
;; how skip re-computation?
;; if props and state is unchanged, use cached value
;; how does this worked for nested components?
;; component identity is based on components path?
;; state updates contain a path
;; each componennt instance has unique id
;; how find relative values?
;; render tree as first class value?
;; - find self, go to parent, find next
;; how do inputs work?

;; instance of a component can close over a value?
;; no more value cache

;; optimize for re-computation how though?
