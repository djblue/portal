(ns portal.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require [portal.styled :as s]
            [react-visibility-sensor :default VisibilitySensor]
            [reagent.core :as r]))

(defn lazy-seq []
  (let [n (r/atom 0)]
    (fn [seqable]
      [:<>
       (take @n seqable)
       (when (seq (drop @n seqable))
         [:> VisibilitySensor
          {:key @n
           :on-change
           #(when % (swap! n + 5))}
          [s/div {:style {:height "1em" :width "1em"}}]])])))
