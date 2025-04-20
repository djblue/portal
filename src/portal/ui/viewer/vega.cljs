(ns ^:no-doc portal.ui.viewer.vega
  "Viewer for the Vega-Lite specification
  https://vega.github.io/vega/docs/specification/"
  (:require ["vega-embed" :as vegaEmbed]
            [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.react :as react]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(def vega-url #"https://vega\.github\.io/schema/vega/v\d\.json")

(s/def ::$schema
  (s/and string? #(re-matches vega-url %)))

(s/def ::vega
  (s/keys :req-un [::$schema]))
;;;

(defn styles
  "CSS styles applied to the vega embed elements. Allow filling most of the container."
  []
  (let [theme (theme/use-theme)]
    [:style
     (d/map->css
      {[:.vega-embed :.chart-wrapper]
       {:width "fit-content"
        :height "fit-content"}
       [:.vega-embed]
       {:width "100%"}
       [:.vega-embed :summary]
       {:opacity 1
        :cursor :default
        :position :absolute
        :right (* 0.5 (:padding theme))
        :top (* 0.5 (:padding theme))
        :z-index 0
        :transform "scale(0.6)"}})]))

(defn- default-config
  "Specifies a nicer set of vega-lite specification styles.
  All defaults can be overridden by users data"
  [theme]
  (let [background (ins/get-background)
        text (::c/text theme)
        border (::c/border theme)
        padding (:padding theme)]
    {:padding {:bottom padding
               :top (* 3 padding)
               :left (* 4 padding)
               :right (* 4 padding)}
     :autosize
     {:type "fit-x" :resize true :contains "padding"}
     :config
     {:legend
      {:labelColor text
       :titleColor text}
      :view
      {:stroke "transparent"}
      :axis
      {:domainColor border
       :domainWidth "3"
       :tickColor border
       :gridColor border
       :gridDash [10 2]
       :titleColor text
       :labelColor text}
      :range {:category (map theme (take 10 theme/order))}}
     :background background}))

(defn- deep-merge
  "Recursively merges maps.
   http://dnaeon.github.io/recursively-merging-maps-in-clojure/"
  [& maps]
  (letfn [(m [& xs]
            (if (some #(and (map? %) (not (record? %))) xs)
              (apply merge-with m xs)
              (last xs)))]
    (reduce m maps)))

(defn- use-resize []
  (let [ref              (react/use-ref)
        [rect set-rect!] (react/use-state #js {:height 200 :width 200})]
    (react/use-effect
     #js [(.-current ref)]
     (when-let [el (.-current ref)]
       (let [resize-observer
             (js/ResizeObserver.
              (fn []
                (set-rect! (.getBoundingClientRect el))))]
         (.observe resize-observer el)
         (fn []
           (.disconnect resize-observer)))))
    [ref rect]))

(defn vega-embed [opts value]
  (let [theme (theme/use-theme)
        doc (deep-merge (default-config theme) value {:title ""})

        view             (react/use-ref)
        [init set-init!] (react/use-state false)

        [absolute absolute-rect] (use-resize)
        height (.-height absolute-rect)

        [relative relative-rect] (use-resize)
        width (.-width relative-rect)]

    (react/use-effect
     #js [(hash theme)]
     (when-let [el (.-current absolute)]
       (-> (vegaEmbed el (clj->js (assoc doc :width width)) (clj->js opts))
           (.then (fn [value]
                    (set! (.-current view) (.-view value))
                    (set-init! true)))
           (.catch (fn [err] (js/console.error err)))))
     #(when-let [view (.-current view)]
        (.finalize view)
        (set! (.-current view) nil)))

    (react/use-effect
     #js [init (.-current view) width]
     (when-let [view (.-current view)]
       (let [width (- width 2
                      (* 2 (:padding theme)))]
         (.width view width)
         (.run view))))

    [d/div
     (when-let [title (:title value)]
       [:h1 title])
     (when-let [description (:description value)]
       [:p description])
     [d/div
      {:ref relative
       :style
       {:min-width 400
        :height height
        :position :relative
        :border [:solid 1 (::c/border theme)]
        :border-radius (:border-radius theme)
        :background (ins/get-background)}}
      [:div#viz
       {:ref absolute
        :style
        {:position :absolute
         :top 0
         :right 0
         :left 0
         :box-sizing :border-box
         :padding (:padding theme)
         :overflow :hidden}}]]]))

(defn vega-viewer [value]
  [vega-embed {:mode "vega" :renderer :svg} value])

(def viewer
  {:predicate (partial s/valid? ::vega)
   :component #'vega-viewer
   :name :portal.viewer/vega})
