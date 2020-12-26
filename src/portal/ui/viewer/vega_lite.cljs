(ns portal.ui.viewer.vega-lite
  (:require ["vega-embed" :as vegaEmbed]
            [clojure.spec.alpha :as sp]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.styled :as s]
            [reagent.core :as r]
            [reagent.dom :as rd]))

;; Vega-lite spec
;; https://vega.github.io/vega-lite/docs/spec.html

(sp/def ::field
  (sp/or :key keyword? :string string?))

(sp/def ::x
  (sp/keys :req-un [::field]
           :opt-un [::type ::title ::axis]))

(sp/def ::y
  (sp/keys :req-un [::field]
           :opt-un [::type ::title ::axis]))

(sp/def ::encoding
  (sp/keys :opt-un [::x ::y ::theta]))

(sp/def ::values
  (sp/coll-of map?))

(sp/def ::inline-data
  (sp/keys :req-un [::values]))

(sp/def ::remote-data
  (sp/keys :req-un [::url]))

(sp/def ::data
  (sp/or :remote-data ::remote-data :inline ::inline-data))

(sp/def ::mark-string
  #{"bar" "circle" "square" "rect" "tick" "line" "area" "point" "geoshape" "rule" "text" "boxplot" "errorband" "errorbar"})

(sp/def ::mark-object
  (sp/keys :req-un [::mark-string]
           :opt-un [::aria ::description ::style ::tooltip ::clip ::invalid ::order]))

(sp/def ::mark
  (sp/or :mark ::mark-string :mark-object ::mark-object))

(sp/def ::layer coll?)

;; https://vega.github.io/vega-lite/docs/spec.html#single
(sp/def ::single-view
  (sp/keys :req-un [::data ::encoding ::mark]
           :opt-un [::title ::name ::description ::transform ::width ::height]))

;; https://vega.github.io/vega-lite/docs/spec.html#layered-and-multi-view-specifications
(sp/def ::layered-view
  (sp/keys :req-un [::data ::encoding ::layer]
           :opt-un [::title ::name ::description ::transform ::width ::height]))

(sp/def ::vega-lite
  (sp/or :single-view ::single-view :layered-view ::layered-view))

(defn- default-config
  "Specifies a nicer set of vega-lite specification styles.
  All defaults can be overridden by users data"
  [settings]
  (let [background (::c/background settings)
        text (::c/text settings)
        border (::c/border settings)]
    {:width "container"
     :height "container"
     :padding "30"
     :autosize
     {:type "fit"
      :resize true
      :contains "padding"}
     :config
     {:legend
      {:labelColor text
       :titleColor text}
      :view
      {:stroke "transparent"}
      :axis
      {:domainColor border
       :domainWidth "3"
       :tickColor border
       :gridColor border
       :gridDash [10 2]
       :titleColor text
       :labelColor text}}
     :background background}))

(defn- map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (s/style->css v) "}\n"))
   ""
   m))

(defn- vega-lite-styles
  "CSS styles applied to the vega embed elements. Allow filling most of the container."
  [settings]
  [:style
   (map->css
    {[:.vega-embed :.chart-wrapper]
     {:width "100%" :height "100%"}
     [:.vega-embed]
     {:width "100%"
      :height "90%"
      :border-color (::c/border settings)
      :border-width 1
      :border-style :solid}
     [:.vega-embed :summary]
     {:opacity 1
      :cursor :default
      :margin-right (:spacing/padding settings)
      :margin-top (:spacing/padding settings)}})])

(defn- deep-merge
  "Recursively merges maps.
   http://dnaeon.github.io/recursively-merging-maps-in-clojure/"
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

;; The following is based on Oz to avoid bringing in extra deps
;; https://github.com/metasoarous/oz/blob/master/src/cljs/oz/core.cljs
(defn- vega-embed
  [elem doc opts]
  (-> (vegaEmbed elem (clj->js doc) (clj->js opts))
      (.catch (fn [err] (js/console.error err)))))

(defn- vega-class
  "Creates React Class component for vega(-lite)"
  [doc opts]
  (r/create-class
   {:display-name
    (:mode doc)
    :component-did-mount
    (fn [this]
      (vega-embed (rd/dom-node this) doc opts))
    :component-will-update
    (fn [this [_ new-doc new-opts]]
      (vega-embed (rd/dom-node this) new-doc new-opts))
    :reagent-render
    (fn [_]
      [:div.viz])}))

(defn vega-lite-viewer
  [settings value]
  [s/div
   [vega-lite-styles settings]
   [:h1 (:title value)]
   [:p (:description value)]
   [vega-class
    (deep-merge (default-config settings) value)
    {:mode "vega-lite" :renderer :canvas}]])

(def viewer
  {:predicate (partial sp/valid? ::vega-lite)
   :component vega-lite-viewer
   :name :portal.viewer/vega-lite})