(ns portal.ssr.ui.inspector
  (:require
   [clojure.string :as str]
   [portal.colors :as c]))

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

(def ^:dynamic *theme* (merge
                        (::c/nord-light c/themes)
                        {:font-family   "monospace"
                         :font-size     "12pt"
                         :string-length 100
                         :max-depth     2
                         :padding       6
                         :border-radius 2}))
(def ^:dynamic *context* nil)

(declare inspect)

(defn- value->css [v]
  (cond
    (number? v)  (str v "px")
    (keyword? v) (name v)
    (vector? v)  (str/join " " (map value->css v))
    (list? v)    (str (first v)
                      "("
                      (str/join ", " (map value->css (rest v)))
                      ")")
    :else        v))

(defn- border [v]
  (str/join " " (map value->css v)))

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
      {:border (border [1 :solid (::c/border *theme*)])
       ;;  :background (get-background)
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
         :border-right (border [1 :solid (::c/border *theme*)])}}
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

      #_(when (seq metadata)
          [coll-action
           {:on-click
            (fn [e]
              (set-show-meta! not)
              (.stopPropagation e))
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
         {:border-top (border [1 :solid (::c/border *theme*)])
          :box-sizing :border-box
          :padding (:padding *theme*)}}
        [inspect metadata]])]))

(defn- container-map [child]
  [:div
   {:style
    {:width "100%"
     :display :grid
     :grid-template-columns "auto 1fr"
     ;;  :background (get-background)
     :grid-gap (:padding *theme*)
     :padding (:padding *theme*)
     :box-sizing :border-box
     :color (::c/text *theme*)
     :font-size  (:font-size *theme*)
     :border-bottom-left-radius (:border-radius *theme*)
     :border-bottom-right-radius (:border-radius *theme*)
     :border-top-right-radius 0
     :border-top-left-radius 0
     :border (border [1 :solid (::c/border *theme*)])}}
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

(defmethod inspect* :default [value] [:span (pr-str value)])

(defn inspect [value]
  (inspect* value))

(comment
  (require '[examples.data :as data])
  (require 'portal.runtime.render)
  (require '[portal.runtime :as rt])

  (require '[portal.api :as p])
  (def ssr (p/open {:mode :ssr}))

  ((get @rt/connections (:session-id ssr))
   (pr-str (portal.runtime.render/render [inspect {:hello :world}])))

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
