(ns ^:no-doc portal.ui.viewer.charts
  (:require [clojure.spec.alpha :as sp]
            [portal.colors :as c]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.vega-lite :as v]))

;; collection of maps of [{:x 0 :y 0} ...] maps
(sp/def :tabular/x number?)
(sp/def :tabular/y number?)
(sp/def ::data
  (sp/keys :req-un [:tabular/x :tabular/y]))
(sp/def ::tabular-data
  (sp/coll-of ::data :min-count 2))

;; :x [0 1 2 ...] :y [0 1 2 ...]
(sp/def :numerical-coll/x
  (sp/coll-of number? :min-count 2))
(sp/def :numerical-coll/y
  (sp/coll-of number? :min-count 2))
(sp/def ::numerical-collection
  (sp/keys :req-un [:numerical-coll/x :numerical-coll/y]))

(defn- normalize-data
  "Normalize data to conform to vega-lite specification"
  [data]
  (cond
    (sp/valid? ::tabular-data data)
    data
    (sp/valid? ::numerical-collection data)
    (for [[x y] (map vector (:x data) (:y data))] {:x x :y y})))

(defn line-chart-viewer [value]
  (let [theme (theme/use-theme)]
    [v/vega-lite-viewer
     {:data
      {:values (normalize-data value)}
      :encoding
      {:x {:field "x" :type "quantitative"}
       :y {:field "y" :type "quantitative"}
       :color {:value (::c/number theme)}}
      :mark "line"
      :selection {:grid {:type "interval" :bind "scales"}}}]))

(defn scatter-chart-viewer [value]
  (let [theme (theme/use-theme)]
    [v/vega-lite-viewer
     {:data {:values (normalize-data value)}
      :encoding
      {:x {:field "x" :type "quantitative"}
       :y {:field "y" :type "quantitative"}
       :color {:value (::c/number theme)}
       :tooltip
       [{:field "x" :type "quantitative"}
        {:field "y" :type "quantitative"}]}
      :mark "circle"
      :selection {:grid {:type "interval" :bind "scales"}}}]))

(defn histogram-chart-viewer [value]
  (let [theme (theme/use-theme)]
    [v/vega-lite-viewer
     {:data {:values (normalize-data value)}
      :mark "bar"
      :encoding
      {:x {:bin true :field "x" :type "quantitative"}
       :y {:aggregate "count" :type "quantitative"}
       :color {:value (::c/number theme)}}}]))

(def line-chart
  {:predicate (partial sp/valid? (sp/or :tabular ::tabular-data ::numerical-collection ::numerical-collection))
   :component line-chart-viewer
   :name :portal.viewer/line-chart})

(def scatter-chart
  {:predicate (partial sp/valid? (sp/or :tabular ::tabular-data ::numerical-collection ::numerical-collection))
   :component scatter-chart-viewer
   :name :portal.viewer/scatter-chart})

(def histogram-chart
  {:predicate (partial sp/valid? (sp/or :tabular ::tabular-data ::numerical-collection ::numerical-collection))
   :component histogram-chart-viewer
   :name :portal.viewer/histogram-chart})
