(ns demo
  (:require
   [reagent.dom :as rdom]
   [portal.web :as pw]))

(defn- app []
  [:<>
   [:div {:style {:display :flex, :justify-content :center}}
    [:button {:on-click #(pw/submit {:hello :world, :n (rand-int 10)})
              :style {:padding 8, :margin 8, :width "100%"}}
     "Test"]]
   [:div
    {:ref
     (fn [x]
       (when x
         (let [iframe (doto (pw/open {:portal.launcher/iframe-parent x})
                        (.setAttribute "style" "width: 100vw; height: 90vh; border: 0"))]
           (.appendChild x iframe))))}]])

(defn ^:dev/after-load init []
  (rdom/render [app] (js/document.getElementById "root")))
