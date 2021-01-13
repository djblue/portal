(ns portal.ui.core
  (:require [portal.ui.app :as app]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.dom :as dom]))

(defn render-app []
  (dom/render [app/app {:send! rpc/request}]
              (.getElementById js/document "root")))

(defn main! []
  (state/long-poll rpc/request)
  (render-app))

(defn reload! [] (render-app))
