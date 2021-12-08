(ns portal.ui.viewer.vega-lite
  "Viewer for the Vega-Lite specification
  https://vega.github.io/vega-lite/docs/spec.html"
  (:require [clojure.spec.alpha :as sp]
            [portal.ui.viewer.vega :as vega]))

(sp/def ::name string?)
(sp/def ::description string?)
(sp/def ::$schema
  (sp/and string? #(re-matches #"https://vega\.github\.io/schema/vega-lite/v\d\.json" %)))

(sp/def ::vega-lite
  (sp/keys :req-un [::data]
           :opt-un [::name ::description ::$schema]))

(defn vega-lite-viewer [value]
  ^{:key (hash value)}
  [vega/vega-embed
   {:mode "vega-lite" :renderer :canvas}
   value])

(def viewer
  {:predicate (partial sp/valid? ::vega-lite)
   :component vega-lite-viewer
   :name :portal.viewer/vega-lite})
