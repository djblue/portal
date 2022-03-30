(ns portal.ui.viewer.plotly
  "Viewer for plotly
  For more information, see:
  https://github.com/plotly/plotly.js
  https://github.com/plotly/react-plotly.js"
  (:require
   [clojure.spec.alpha :as sp]
   ["react-plotly.js$default" :as plotly]))

(sp/def ::name string?)
(sp/def ::plotly
  (sp/keys :req-un [::data]
           :opt-un [::name ::layout ::config ::style]))

(defn plotly-viewer [value]
  ^{:key (hash value)}
  [:div {:style {:display :flex :justify-content :flex-end}}
   [:> plotly (merge {:style {:width 960 :height 540}} value)]])

(def viewer
  {:predicate (partial sp/valid? ::plotly)
   :component plotly-viewer
   :name :portal.viewer/plotly})

(comment
  (tap> {:data [{:x [0 2] :y [0 3]}]
         :style {:width 960 :height 560}})
  (tap> {:data [{:values [0 2 3 4 5]
                 :type :pie}]
         :style {:width "100%" :height "100%"}})
  )
