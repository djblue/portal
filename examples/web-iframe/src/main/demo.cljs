(ns demo
  (:require
   [reagent.dom :as rdom]

   [portal.runtime.web.client :as c]
   [portal.runtime.web.launcher :as pwl]
   [portal.web :as pw]))

(defn- portal-iframe []
  [:iframe {:style {:width "100vw" :height "100vh" :border 0}
            :src (pwl/iframe-url {})
            :ref (fn [x]
                   (when x
                     (-> x .-contentWindow .-window .-opener (set! js/window))
                     (reset! c/connection (-> x .-contentWindow))
                     (pw/submit {:hello :world})))}])

(defn ^:dev/after-load init []
  (rdom/render [portal-iframe] (js/document.getElementById "root")))
