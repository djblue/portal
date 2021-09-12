(ns portal.ui.viewer.vega
  (:require ["react" :as react]
            ["vega-embed" :as vegaEmbed]
            [clojure.spec.alpha :as sp]
            [clojure.string :as str]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(sp/def ::$schema
  (sp/and string? #(re-matches #"https://vega\.github\.io/schema/vega/v\d\.json" %)))

(sp/def ::vega
  (sp/keys :req-un [::$schema]))

(defn- map->css [m]
  (reduce-kv
   (fn [css k v]
     (str css
          (str/join " " (map name k))
          "{" (s/style->css v) "}\n"))
   ""
   m))

(defn- vega-styles
  "CSS styles applied to the vega embed elements. Allow filling most of the container."
  []
  (let [theme (theme/use-theme)]
    [:style
     (map->css
      {[:.vega-embed :.chart-wrapper]
       {:width "fit-content"
        :height "fit-content"}
       [:.vega-embed]
       {:width "100%"}
       [:.vega-embed :summary]
       {:opacity 1
        :cursor :default
        :position :absolute
        :right (:padding theme)
        :top (:padding theme)}})]))

(defn- default-config
  "Specifies a nicer set of vega-lite specification styles.
  All defaults can be overridden by users data"
  [theme]
  (let [background (ins/get-background)
        text (::c/text theme)
        border (::c/border theme)]
    {:padding "30"
     :autosize
     {:type "fit" :resize true :contains "padding"}
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
       :labelColor text}}
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
  (let [ref              (react/useRef nil)
        [rect set-rect!] (react/useState #js {:height 200 :width 200})]
    (react/useEffect
     (fn []
       (when-let [el (.-current ref)]
         (let [resize-observer
               (js/ResizeObserver.
                (fn []
                  (set-rect! (.getBoundingClientRect el))))]
           (.observe resize-observer el)
           (fn []
             (.disconnect resize-observer)))))
     #js [(.-current ref)])
    [ref rect]))

(defn vega-embed [opts value]
  (let [theme (theme/use-theme)
        doc (deep-merge (default-config theme) value {:title ""})

        view             (react/useRef nil)
        [init set-init!] (react/useState false)

        [absolute absolute-rect] (use-resize)
        height (.-height absolute-rect)

        [relative relative-rect] (use-resize)
        width (.-width relative-rect)]

    (react/useEffect
     (fn []
       (when-let [el (.-current absolute)]
         (-> (vegaEmbed el (clj->js (assoc doc :width width)) (clj->js opts))
             (.then (fn [value]
                      (set! (.-current view) (.-view value))
                      (set-init! true)))
             (.catch (fn [err] (js/console.error err)))))
       #(when-let [view (.-current view)]
          (.finalize view)
          (set! (.-current view) nil)))
     #js [])

    (react/useEffect
     (fn []
       (when-let [view (.-current view)]
         (let [width (- width 2
                        (* 2 (:padding theme)))]
           (.width view width)
           (.height view (* width 0.8))
           (.run view))))
     #js [init (.-current view) width])

    [s/div
     [vega-styles]
     (when-let [title (:title value)]
       [:h1 title])
     (when-let [description (:description value)]
       [:p description])
     [s/div
      {:ref relative
       :style
       {:width "100%"
        :min-width 400
        :height height
        :position :relative
        :border [:solid 1 (::c/border theme)]
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
  ^{:key (hash value)}
  [vega-embed {:mode "vega" :renderer :canvas} value])

(def viewer
  {:predicate (partial sp/valid? ::vega)
   :component vega-viewer
   :name :portal.viewer/vega})
