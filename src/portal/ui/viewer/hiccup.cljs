(ns ^:no-doc portal.ui.viewer.hiccup
  (:require [portal.colors :as c]
            [portal.ui.filter :as-alias f]
            [portal.ui.inspector :as ins]
            [portal.ui.lazy :as l]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

(defn- header-styles [theme]
  {:color (::c/namespace theme)
   :margin 0
   :padding-top (:padding theme)
   :padding-bottom (:padding theme)})

(defn styles []
  (let [theme (theme/use-theme)
        h     (header-styles theme)
        bg    (ins/get-background)
        bg2   (ins/get-background2)
        border-bottom
        {:border-bottom
         (str "1px solid " (::c/border theme))}]
    [:style
     (d/map->css
      {[:h1.hiccup] (merge {:font-size "2em"} h border-bottom)
       [:h2.hiccup] (merge {:font-size "1.5em"} h border-bottom)

       [:h3.hiccup] h [:h4.hiccup] h [:h5.hiccup] h [:h6.hiccup] h

       [:a.hiccup] {:color (::c/uri theme) :width :fit-content}

       [:ul.hiccup] {:margin 0}
       [:ol.hiccup] {:margin 0}
       [:li.hiccup] {:margin-top (:padding theme)}

       [:p.hiccup]
       {:font-family "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji"
        :font-size "1.1em"
        :line-height "1.5em"
        :margin 0}

       [:img.hiccup] {:max-width "100%"}
       [:hr.hiccup] {:width "calc(100% - 2px)"
                     :border [1 :solid (::c/border theme)]
                     :border-radius (:border-radius theme)}

       [:code.hiccup]
       {:background bg2
        :display :inline-block
        :border-radius (:border-radius theme)
        :border [1 :solid (::c/border theme)]
        :box-sizing :border-box
        :padding [0 (* 1.5 (:padding theme))]}

       [:blockquote.hiccup]
       {:margin 0
        :background bg
        :padding-left (* 2 (:padding theme))
        :padding-bottom (:padding theme)
        :padding-top (:padding theme)
        :font-style :normal
        :border-left [(:padding theme) :solid (::c/border theme)]}

       [:table.hiccup]
       {:margin 1
        :color (::c/text theme)
        :display :block
        :font-size (:font-size theme)
        :overflow :auto
        :max-width "calc(100% - 2px)"
        :width :max-content
        :border-style :hidden
        :border-collapse :collapse
        :border-radius (:border-radius theme)}

       [:th.hiccup]
       {:font-weight :bold
        :text-align :center
        :padding (* 1.5 (:padding theme))
        :border [1 :solid (::c/border theme)]}
       [:td.hiccup]
       {:text-align :left
        :padding (:padding theme)
        :border [1 :solid (::c/border theme)]}

       ["tr.hiccup:nth-child(odd)"]  {:background bg}
       ["tr.hiccup:nth-child(even)"] {:background bg2}

       ["div.hiccup-root"]
       {:display :flex
        :flex-direction :column
        :gap (* 1.5 (:padding theme))}})]))

(defn inspect-code [& args]
  (let [[_ attrs code] (second args)
        theme          (theme/use-theme)]
    [d/div
     {:style
      {:margin-top    (:padding theme)
       :margin-bottom (:padding theme)}}
     [ins/inspector
      {:portal.viewer/default :portal.viewer/code
       :portal.viewer/code {:language (:class attrs)}}
      code]]))

(defn- inspect-inline-code [& args]
  (into [:code {:class "hiccup"
                :style {:background (ins/get-background2)}}]
        (rest args)))

(def tag->viewer
  {:pre inspect-code
   :code inspect-inline-code})

(defn- process-hiccup [context hiccup]
  (if-not (vector? hiccup)
    hiccup
    (let [[tag & args]  hiccup
          component     (or (when (get-in context [:viewers tag :component])
                              ins/inspector)
                            (tag->viewer tag))]
      (if component
        (let [row (swap! (:count context) inc)]
          [ins/with-key
           row
           [select/with-position
            {:row row :column 0}
            (if (= tag :portal.viewer/inspector)
              (into [component] args)
              (if (= 1 (count args))
                (into [component {:portal.viewer/default tag}] args)
                (into [component (merge (first args) {:portal.viewer/default tag})] (rest args))))]])
        (if (map? (first args))
          (into
           (if (= tag :<>)
             [tag (first args)]
             [tag (update (first args) :class str " hiccup")])
           (map #(process-hiccup context %))
           (rest args))
          (into
           (if (= tag :<>)
             [tag]
             [tag {:class "hiccup"}])
           (map #(process-hiccup context %))
           args))))))

(defn- inspect-hiccup* [value]
  (let [viewers (ins/viewers-by-name @ins/viewers)
        opts    (ins/use-options)]
    [ins/toggle-bg
     [d/div
      {:class "hiccup-root"
       :style
       {:overflow   :auto
        :max-height (when-not (:expanded? opts) "24rem")}}
      (process-hiccup
       {:count (atom -1) :viewers viewers}
       (if-let [[tag attrs] value]
         (let [missing-tag?   (not= tag (first value))
               missing-attrs? (and (map? attrs)
                                   (not= attrs (second value)))]
           (cond-> [] missing-tag? (conj tag) missing-attrs? (conj attrs) :always (into value)))
         value))]]))

(defn inspect-hiccup [value]
  [l/lazy-render [inspect-hiccup* value]])

(defn hiccup? [value]
  (and (vector? value)
       (keyword? (first value))))

(def viewer
  {:predicate hiccup?
   :component #'inspect-hiccup
   :name :portal.viewer/hiccup
   :doc "Render a hiccup value as html via reagent."})
