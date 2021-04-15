(ns portal.ui.core
  (:require [portal.ui.app :as app]
            [portal.ui.connecton-status :as conn]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn connected-app []
  [conn/with-status [app/app]])

(defn render-app []
  (dom/render [connected-app]
              (.getElementById js/document "root")
              functional-compiler))

(defn main! []
  (reset! state/sender rpc/request)
  (state/long-poll rpc/request)
  (render-app))

(defn reload! [] (render-app))
