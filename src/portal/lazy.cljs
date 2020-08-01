(ns portal.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require [portal.styled :as s]
            [react-visibility-sensor :default VisibilitySensor]
            [reagent.core :as r]))

(defn lazy-seq [_ opts]
  (let [{:keys [default-take step]
         :or   {default-take 0 step 5}} opts
        n (r/atom default-take)]
    (fn [seqable]
      [:<>
       (take @n seqable)
       (when (seq (drop @n seqable))
         [:> VisibilitySensor
          {:key @n
           :on-change
           #(when % (swap! n + step))}
          [s/div {:style {:height "1em" :width "1em"}}]])])))
