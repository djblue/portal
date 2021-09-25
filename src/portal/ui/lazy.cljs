(ns portal.ui.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require ["react" :as react]
            [reagent.core :as r]))

(defn- visible? [entries]
  (= 1 (reduce
        (fn [sum entry]
          (if-not (.-isIntersecting entry)
            sum
            (+ sum (.-intersectionRatio entry)))) 0 entries)))

(defn- visible-sensor [f]
  (let [ref (react/useRef nil)]
    (react/useEffect
     (fn []
       (when (.-current ref)
         (let [observer
               (js/IntersectionObserver.
                (fn [entries]
                  (when (visible? entries) (f)))
                #js {:root nil :rootMargin "0px" :threshold 1.0})]
           (.observe observer (.-current ref))
           (fn [] (.unobserve observer (.-current ref)))))
       #js [(.-current ref) f]))
    [:div {:ref ref :style {:height "1em" :width "1em"}}]))

(defn lazy-seq [_coll opts]
  (let [{:keys [default-take step]
         :or   {default-take 0 step 10}} opts
        n (r/atom default-take)]
    (fn [coll _opts]
      (let [[head tail] (split-at @n coll)]
        [:<>
         head
         (when (seq tail)
           [visible-sensor
            (fn [] (swap! n + step))])]))))
