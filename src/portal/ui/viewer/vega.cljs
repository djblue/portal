(ns portal.ui.viewer.vega
  (:require ["vega-embed" :as vegaEmbed]
            [clojure.spec.alpha :as sp]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]
            [reagent.dom :as rd]))

(sp/def ::$schema
  (sp/and string? #(re-matches #"https://vega\.github\.io/schema/vega/v\d\.json" %)))

(sp/def ::vega
  (sp/keys :req-un [::$schema]))

(defn- map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (s/style->css v) "}\n"))
   ""
   m))

(defn- vega-styles
  "CSS styles applied to the vega embed elements. Allow filling most of the container."
  []
  (let [theme (theme/use-theme)]
    [:style
     (map->css
      {[:.vega-embed :.chart-wrapper]
       {:width "100%" :height "80%"}
       [:.vega-bindings]
       {:padding 20}
       [:.vega-embed]
       {:width "100%"
        :height "80%"
        :border-color (::c/border theme)
        :border-width 1
        :border-style :solid}
       [:.vega-embed :summary]
       {:opacity 1
        :cursor :default
        :margin-right (:spacing/padding theme)
        :margin-top (:spacing/padding theme)}})]))

(defn- deep-merge
  "Recursively merges maps.
   http://dnaeon.github.io/recursively-merging-maps-in-clojure/"
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn- vega-embed
  [elem doc opts]
  (-> (vegaEmbed elem (clj->js doc) (clj->js opts))
      (.catch (fn [err] (js/console.error err)))))

(defn- new-size []
  {:width (max 650 (* 0.9 (js/parseInt (.-innerWidth js/window))))
   :height (max 200 (* 0.55 (js/parseInt (.-innerHeight js/window))))})

(def ^:private view-size
  (r/atom (new-size)))

(defn- vega-class
  "Creates React Class component for vega(-lite)"
  [doc opts]
  (r/create-class
   {:display-name
    (:mode doc)
    :component-did-mount
    (fn [this]
      ;; componenet will listen to window resize events to update it's size
      (js/window.addEventListener "resize" #(reset! view-size (new-size)))
      (vega-embed (rd/dom-node this) doc opts))
    :component-will-update
    (fn [this [_ new-doc new-opts]]
      (vega-embed (rd/dom-node this) new-doc new-opts))
    :reagent-render
    (fn [_]
      [:div#viz])}))

(defn vega-viewer
  [value]
  [s/div
   [vega-styles]
   [:h1 (:title value)]
   [:p (:description value)]
   [vega-class
    (deep-merge value @view-size {:title ""})
    {:mode "vega" :renderer :canvas}]])

(def viewer
  {:predicate (partial sp/valid? ::vega)
   :component vega-viewer
   :name :portal.viewer/vega})
