(ns ^:no-doc portal.ui.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require ["react" :as react]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn- observer-visible? [entries]
  (= 1 (reduce
        (fn [sum entry]
          (if-not (.-isIntersecting entry)
            sum
            (+ sum (.-intersectionRatio entry)))) 0 entries)))

(defn- observer-visible-sensor [f]
  (let [ref (react/useRef nil)]
    (react/useEffect
     (fn []
       (when (.-current ref)
         (let [observer
               (js/IntersectionObserver.
                (fn [entries]
                  (when (observer-visible? entries) (f)))
                #js {:root nil :rootMargin "0px" :threshold 1.0})]
           (.observe observer (.-current ref))
           (fn [] (.unobserve observer (.-current ref)))))
       #js [(.-current ref) f]))
    [:div {:ref ref :style {:height "1em" :width "1em"}}]))

(defn element-visible? [element]
  (let [buffer 100
        rect   (.getBoundingClientRect element)
        height (or (.-innerHeight js/window)
                   (.. js/document -documentElement -clientHeight))
        width  (or (.-innerWidth js/window)
                   (.. js/document -documentElement -clientWidth))]
    (and (> (.-bottom rect) buffer)
         (<= (.-top rect) (- height buffer))
         (> (.-left rect) 0)
         (<= (.-right rect) width))))

(defn- fallback-visible-sensor [f]
  (let [ref       (react/useRef nil)
        container (:scroll-element @(state/use-state))]
    (react/useEffect
     (fn []
       (when (some-> ref .-current element-visible?)
         (f))
       (when container
         (let [on-scroll
               (fn []
                 (when (some-> ref .-current element-visible?)
                   (f)))]
           (.addEventListener ^js container "scroll" on-scroll)
           #(.removeEventListener ^js container "scroll" on-scroll))))
     #js [(.-current ref) f container])
    [:div {:ref ref :style {:height "1em" :width "1em"}}]))

(defn visible-sensor [f]
  (if (exists? js/IntersectionObserver)
    [observer-visible-sensor f]
    [fallback-visible-sensor f]))

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
