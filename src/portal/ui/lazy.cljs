(ns portal.ui.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require ["react" :as react]
            ["react-visibility-sensor" :as react-visibility-sensor]
            [portal.ui.styled :as s]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn lazy-seq [_ opts]
  (let [{:keys [default-take step]
         :or   {default-take 0 step 10}} opts
        n (r/atom default-take)
        VisibilitySensor (.-default react-visibility-sensor)]
    (fn [seqable]
      (let [state (state/use-state)
            more? (seq (drop @n seqable))]
        (react/useEffect
         (fn []
           (when-not more?
             (state/dispatch! state state/more)))
         #js [@n])
        [:<>
         (doall (take @n seqable))
         (when more?
           [:> VisibilitySensor
            {:key @n
             :on-change
             #(when % (swap! n + step))}
            [s/div {:style {:height "1em" :width "1em"}}]])]))))
