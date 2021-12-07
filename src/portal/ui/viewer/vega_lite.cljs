(ns portal.ui.viewer.vega-lite
  (:require [clojure.spec.alpha :as sp]
            [portal.ui.viewer.vega :as vega]))

;; Vega-lite spec
;; https://vega.github.io/vega-lite/docs/spec.html

(sp/def ::field
  (sp/or :key keyword? :string string?))

(sp/def ::x
  (sp/keys :req-un [::field]
           :opt-un [::type ::title ::axis]))

(sp/def ::y
  (sp/keys :opt-un [::field ::type ::title ::axis]))

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

(sp/def :portal.ui.viewer.vega-lite.mark/type
  #{"bar" "circle" "square" "rect" "tick" "line" "area" "point" "geoshape" "rule" "text" "boxplot" "errorband" "errorbar"})

(sp/def ::mark-object
  (sp/keys :req-un [:portal.ui.viewer.vega-lite.mark/type]
           :opt-un [::aria ::description ::style ::tooltip ::clip ::invalid ::order]))

(sp/def ::mark
  (sp/or :mark :portal.ui.viewer.vega-lite.mark/type :mark-object ::mark-object))

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

(defn vega-lite-viewer [value]
  ^{:key (hash value)}
  [vega/vega-embed
   {:mode "vega-lite" :renderer :canvas}
   value])

(def viewer
  {:predicate (partial sp/valid? ::vega-lite)
   :component vega-lite-viewer
   :name :portal.viewer/vega-lite})
