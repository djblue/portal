(ns portal.ui.viewer.vega-lite
  (:require [clojure.spec.alpha :as sp]
            [portal.ui.viewer.vega :as vega]))

(sp/def ::$schema
  (sp/and string? #(re-matches #"https://vega\.github\.io/schema/vega-lite/v\d\.json" %)))

(sp/def ::vega-lite
  (sp/keys :req-un [::$schema]))

(defn vega-lite-viewer [value]
  ^{:key (hash value)}
  [vega/vega-embed
   {:mode "vega-lite" :renderer :canvas}
   value])

(def viewer
  {:predicate (partial sp/valid? ::vega-lite)
   :component vega-lite-viewer
   :name :portal.viewer/vega-lite})
