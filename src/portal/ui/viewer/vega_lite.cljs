(ns portal.ui.viewer.vega-lite
  "Viewer for the Vega-Lite specification
  https://vega.github.io/vega-lite/docs/spec.html"
  (:require [clojure.spec.alpha :as s]
            [portal.ui.viewer.vega :as vega]))

;;; :spec
(s/def ::name string?)
(s/def ::description string?)
(s/def ::$schema
  (s/and string? #(re-matches #"https://vega\.github\.io/schema/vega-lite/v\d\.json" %)))

(s/def ::vega-lite
  (s/keys :req-un [::data]
          :opt-un [::name ::description ::$schema]))
;;;

(defn vega-lite-viewer [value]
  [vega/vega-embed
   {:mode "vega-lite" :renderer :svg}
   value])

(def viewer
  {:predicate (partial s/valid? ::vega-lite)
   :component vega-lite-viewer
   :name :portal.viewer/vega-lite})
