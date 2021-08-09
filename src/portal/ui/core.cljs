(ns portal.ui.core
  (:require ["react" :as react]
            [portal.ui.app :as app]
            [portal.ui.connecton-status :as conn]
            [portal.ui.rpc :as rpc]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn use-invoke [f & args]
  (let [[value set-value!] (react/useState ::loading)
        versions           (.from
                            js/Array
                            (map
                             (fn [value]
                               (get @rpc/versions
                                    value
                                    (if (= value ::loading) -1 0)))
                             args))]
    (react/useEffect
     (fn []
       (when (not-any? #{::loading} args)
         (-> (apply state/invoke f args)
             (.then #(set-value! %)))))
     versions)
    value))

(defn use-tap-list []
  (let [a (use-invoke 'portal.runtime/get-tap-atom)]
    (use-invoke 'clojure.core/deref a)))

(defn connected-app []
  [conn/with-status
   [app/app (use-tap-list)]])

(defn with-cache [& children]
  (into [:<> (meta @state/value-cache)] children))

(defn render-app []
  (dom/render [with-cache [connected-app]]
              (.getElementById js/document "root")
              functional-compiler))

(defn main! []
  (reset! state/sender rpc/request)
  (render-app))

(defn reload! [] (render-app))
