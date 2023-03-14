(ns user
  (:require [portal.api :as p]))

(defn open []
  (p/open
   {:app false
    :value
    ^{:portal.viewer/default :portal.viewer/plotly}
    {:data
     [{:x [1 2 3]
       :y [2 6 3]
       :type "scatter"
       :mode "lines+markers"
       :marker {:color "red"}}
      {:type "bar" :x [1 2 3] :y [2 5 3]}]}
    :on-load
    (fn []
      (p/eval-str "(require 'portal.ui.viewer.plotly)"))}))
