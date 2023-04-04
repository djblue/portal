(ns ^:no-doc portal.ui.core
  (:require ["react" :as react]
            [portal.ui.app :as app]
            [portal.ui.cljs :as cljs]
            [portal.ui.connecton-status :as conn]
            [portal.ui.inspector :as ins]
            [portal.ui.options :as opts]
            [portal.ui.rpc :as rpc]
            [portal.ui.sci]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn- custom-app [opts]
  (let [[app set-app!] (react/useState nil)]
    (react/useEffect
     (fn []
       (let [component
             (-> {:code (str "(require '" (namespace (:main opts)) ")" (:main opts))}
                 (cljs/eval-string)
                 :value)]
         (set-app! (fn [] component))))
     #js [])
    (when app
      [app/root [app]])))

(defn connected-app []
  (let [opts (opts/use-options)]
    [conn/with-status
     (cond
       (= opts ::opts/loading) nil
       (contains? opts :main) [app/root
                               [:> ins/error-boundary
                                [custom-app opts]]]
       :else [app/app (:value opts)])]))

(defn with-cache [& children]
  (into [:<> (meta @state/value-cache)] children))

(defn render-app []
  (dom/render [with-cache
               [opts/with-options
                [connected-app]]]
              (.getElementById js/document "root")
              functional-compiler))

(defn main! []
  (cljs/init)
  (reset! state/sender rpc/request)
  (render-app))

(defn reload! [] (render-app))
