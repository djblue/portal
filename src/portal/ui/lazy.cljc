(ns ^:no-doc portal.ui.lazy
  #?(:clj (:refer-clojure :exclude [lazy-seq random-uuid]))
  #?(:clj (:require
           [portal.ssr.ui.react :as react]
           [portal.ssr.ui.uuid :refer [random-uuid]])))

#?(:clj
   (defn lazy-seq
     ([coll]
      (lazy-seq coll nil))
     ([coll {:keys [default-take step]
             :or   {default-take 0 step 10}}]
      (let [[n set-n!] (react/use-state default-take)
            [head tail] (split-at n coll)]
        [:<>
         head
         (when (seq tail)
           [:visible-sensor
            {:id (random-uuid)
             :on-visible (fn [_] (set-n! (+ n step)))}])]))))

#?(:clj
   (defn use-visible []
     (let [[visible? set-visible!] (react/use-state false)]
       [(when-not visible?
          [:visible-sensor
           {:id (random-uuid)
            :on-visible (fn [_] (set-visible! true))}])
        visible?])))

(defmacro use-lazy [k value] `(use-lazy* ~k (fn [] ~value)))
