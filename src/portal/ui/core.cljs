(ns portal.ui.core
  (:require [portal.ui.app :as app]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn render-app []
  (dom/render [app/app]
              (.getElementById js/document "root")
              functional-compiler))

(defn main! []
  (reset! state/sender rpc/request)
  (state/long-poll rpc/request)
  (render-app))

(defn reload! [] (render-app))
