(ns ^:no-doc portal.ui.lazy
  (:refer-clojure :exclude [lazy-seq])
  (:require ["react" :as react]
            [portal.ui.react :refer [use-effect]]
            [portal.ui.state :as state]
            [reagent.core :as r])
  (:require-macros portal.ui.lazy))

(defn- observer-visible? [entries]
  (= 1 (reduce
        (fn [sum entry]
          (if-not (.-isIntersecting entry)
            sum
            (+ sum (.-intersectionRatio entry)))) 0 entries)))

(defn- observer-visible-sensor [f]
  (let [ref (react/useRef nil)]
    (use-effect
     #js [(.-current ref) f]
     (when (.-current ref)
       (let [observer
             (js/IntersectionObserver.
              (fn [entries]
                (when (observer-visible? entries) (f)))
              #js {:root nil :rootMargin "0px" :threshold 1.0})]
         (.observe observer (.-current ref))
         (fn []
           (when (.-current ref)
             (.unobserve observer (.-current ref)))))))
    [:div {:ref ref :style {:height "1em" :width "1em"}}]))

(defn element-visible? [element]
  (let [buffer 100
        rect   (.getBoundingClientRect element)
        height (or (.-innerHeight js/window)
                   (.. js/document -documentElement -clientHeight))
        width  (or (.-innerWidth js/window)
                   (.. js/document -documentElement -clientWidth))]
    (and (> (.-bottom rect) buffer)
         (< (.-top rect) (- height buffer))
         (> (.-right rect) buffer)
         (< (.-left rect) (- width buffer)))))

(defn- fallback-visible-sensor [f]
  (let [ref       (react/useRef nil)
        container (:scroll-element @(state/use-state))]
    (use-effect
     #js [(.-current ref) f container]
     (when (some-> ref .-current element-visible?)
       (f))
     (when container
       (let [on-scroll
             (fn []
               (when (some-> ref .-current element-visible?)
                 (f)))]
         (.addEventListener ^js container "scroll" on-scroll)
         #(.removeEventListener ^js container "scroll" on-scroll))))
    [:div {:ref ref :style {:height "1em" :width "1em"}}]))

(defn visible-sensor [f]
  (if (exists? js/IntersectionObserver)
    [observer-visible-sensor f]
    [fallback-visible-sensor f]))

(defn lazy-seq [_coll opts]
  (let [{:keys [default-take step]
         :or   {default-take 0 step 10}} opts

        state (state/use-state)
        path  (-> opts :context :stable-path)
        n     (if-not path
                (r/atom default-take)
                (r/cursor state [:lazy-take path]))]
    (fn [coll _opts]
      (let [[head tail] (split-at (or @n default-take) coll)]
        [:<>
         head
         (when (seq tail)
           [visible-sensor
            (fn [] (swap! n (fnil + default-take) step))])]))))

(defn use-lazy* [k f] (react/useMemo (fn [] [lazy-seq (f)]) #js [k]))

(defn lazy-render [child]
  (let [[show set-show!] (react/useState false)]
    (if show
      child
      [:<>
       [visible-sensor #(set-show! true)]
       [:div {:style {:height "50vh"}}]])))
