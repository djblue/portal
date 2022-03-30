(ns portal.ui.viewer.plotly
  "Viewer for plotly
  https://github.com/plotly/plotly.js specification"
  (:require
   [clojure.spec.alpha :as sp]
   ["react-plotly.js$default" :as plotly]))

(sp/def ::name string?)
(sp/def ::plotly
  (sp/keys :req-un [::data]
           :opt-un [::name ::layout ::config]))

(defn plotly-viewer [value]
  ^{:key (hash value)}
  [:div {:style {:display :flex :justify-content :flex-end}}
   [:> plotly (merge {:style {:width 960 :height 540}} value)]])

(def viewer
  {:predicate (partial sp/valid? ::plotly)
   :component plotly-viewer
   :name :portal.viewer/plotly})
