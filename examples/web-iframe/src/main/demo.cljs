(ns demo
  (:require
   [reagent.dom :as rdom]
   [portal.web :as pw]))

(defn- app []
  [:div {:style {:display :flex, :height "100vh", :flex-direction "column"}}
   [:div {:style {:display :flex, :justify-content :center}}
    [:button {:on-click #(pw/submit {:hello :world, :n (rand-int 10)})
              :style {:padding 8, :margin 8, :width "100%"}}
     "Test"]]
   [:div {:style {:flex-grow 1}
          :ref (fn [el] (when el (pw/open {:iframe-parent el})))}]])

(defn ^:dev/after-load init []
  (rdom/render [app] (js/document.getElementById "root")))
