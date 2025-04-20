(ns ^:no-doc portal.ui.core
  (:require [portal.async :as a]
            [portal.extensions.vs-code-notebook :as notebook]
            [portal.ui.api :as api]
            [portal.ui.app :as app]
            [portal.ui.cljs :as cljs]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as opts]
            [portal.ui.react :as react]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn- custom-app [opts]
  (let [[app set-app!] (react/use-state nil)]
    (react/use-effect
     :once
     (a/let [_   (cljs/eval-string {:code (str "(require '" (namespace (:main opts)) ")")})
             res (cljs/eval-string {:code (str (:main opts)) :context :expr})]
       (set-app! (fn [] (:value res)))))
    (when app
      [app/root [app]])))

(defn connected-app []
  (let [opts (opts/use-options)]
    (cond
      (= opts ::opts/loading) nil
      (contains? opts :main) [app/root
                              [:> ins/error-boundary
                               [custom-app opts]]]
      :else [app/app (:value opts)])))

(defn with-cache [& children]
  (into [:<> (meta @state/value-cache)] children))

(defn render-app []
  (when-let [el (.getElementById js/document "root")]
    (dom/render [with-cache
                 [opts/with-options
                  [connected-app]]]
                el
                functional-compiler)))

(defn main! []
  (cljs/init)
  (reset! state/sender rpc/request)
  (reset! state/render #'render-app)
  (render-app))

(defn reload! [] (render-app))

(set! (.-embed api/portal-api) notebook/activate)