(ns ^:no-doc portal.ui.viewer.prepl
  (:require ["anser" :as anser]
            [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.runtime.edn :as edn]
            [portal.ui.filter :as f]
            [portal.ui.html :as h]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::tag #{:out :err :tap :ret})

(s/def ::out
  (s/keys :req-un [::tag ::val]))

(s/def ::prepl
  (s/coll-of ::out :min-count 1))
;;;

(defn styles []
  (let [theme (theme/use-theme)]
    [:style
     (d/map->css
      {[:.ansi-black-fg]   {:color (::c/border theme)}
       [:.ansi-black-bg]   {:background (::c/border theme)}
       [:.ansi-red-fg]     {:color (::c/exception theme)}
       [:.ansi-red-bg]     {:background (::c/exception theme)}
       [:.ansi-green-fg]   {:color (::c/string theme)}
       [:.ansi-green-bg]   {:background (::c/string theme)}
       [:.ansi-yellow-fg]  {:color (::c/tag theme)}
       [:.ansi-yellow-bg]  {:background (::c/tag theme)}
       [:.ansi-blue-fg]    {:color (::c/boolean theme)}
       [:.ansi-blue-bg]    {:background (::c/boolean theme)}
       [:.ansi-magenta-fg] {:color (::c/number theme)}
       [:.ansi-magenta-bg] {:background (::c/number theme)}
       [:.ansi-cyan-fg]    {:color (::c/package theme)}
       [:.ansi-cyan-bg]    {:background (::c/package theme)}
       [:.ansi-white-fg]   {:color (::c/text theme)}
       [:.ansi-white-bg]   {:background (::c/text theme)}
       [:.ansi-bold]       {:font-weight :bold}})]))

(defn- inspect-prepl-ret [value index]
  (let [theme (theme/use-theme)
        row   (* 2 index)]
    [ins/with-collection
     value
     [d/div
      (when-let [form (:form value)]
        [ins/with-key
         :form
         [select/with-position
          {:row row :column 0}
          [ins/inspector
           {:portal.viewer/code {:language "clojure"}
            :portal.viewer/default :portal.viewer/code}
           form]]])
      [d/div
       {:style {:display :flex
                :position :relative
                :gap (:padding theme)}}
       (when (= :ret (:tag value))
         [icons/sign-out-alt
          {:style
           {:top 0
            :position :sticky
            :height :fit-content
            :padding [(:padding theme) 0]
            :color (if (:exception value)
                     (::c/exception theme)
                     (::c/border theme))}}])
       [ins/with-key
        :val
        [select/with-position
         {:row (inc row) :column 0}
         [d/div
          {:style {:width "100%"
                   :padding
                   [(:padding theme) 0]}}
          [ins/inspector
           (try
             (edn/read-string (:val value))
             (catch :default _ (:val value)))]]]]
       (when-let [ms (:ms value)]
         (when (> ms 100)
           [ins/with-key
            :ms
            [select/with-position
             {:row (inc row) :column 1}
             [d/div
              {:style {:top 0
                       :position :sticky
                       :height :fit-content
                       :padding [(:padding theme) 0]}}
              [ins/inspector
               {:portal.viewer/default :portal.viewer/duration-ms}
               ms]]]]))]]]))

(defn- has-exception? [value] (some :exception value))

(defn- title-bar-actions [value]
  (let [theme    (theme/use-theme)
        state    (state/use-state)
        context  (ins/use-context)
        location (state/get-location context)
        ex?      (has-exception? value)
        border   (if ex?
                   (::c/exception theme)
                   (::c/border theme))]
    [d/div
     {:style
      {:display       :flex
       :gap           (:padding theme)
       :box-sizing    :border-box
       :position      :relative
       :padding       (* 1.6 (:padding theme))
       :border        [1 :solid border]
       :z-index       1
       :border-top-left-radius (:border-radius theme)
       :border-top-right-radius (:border-radius theme)}}

     (when ex?
       [d/div
        {:style
         {:opacity    0.15
          :position :absolute
          :left 0
          :right 0
          :top 0
          :bottom 0
          :z-index -1
          :background (::c/exception theme)}}])

     [icons/times-circle
      {:size "1x"
       :style {:opacity 0.75
               :color (::c/exception theme)}
       :style/hover {:opacity 1}
       :title "Click to do nothing. 😊"
       :on-click (fn [e]
                   (.stopPropagation e))}]

     [icons/minus-circle
      {:size "1x"
       :style {:opacity 0.75
               :cursor :pointer
               :color (::c/tag theme)}
       :style/hover {:opacity 1}
       :title "Click to collapse."
       :on-click (fn [e]
                   (.stopPropagation e)
                   (state/dispatch! state assoc-in [:expanded? location] 0))}]

     [icons/plus-circle
      {:size "1x"
       :style {:opacity 0.75
               :cursor :pointer
               :color (::c/string theme)}
       :style/hover {:opacity 1}
       :title "Click to expand."
       :on-click (fn [e]
                   (.stopPropagation e)
                   (state/dispatch! state assoc-in [:expanded? location] 1))}]]))

(defn- escape-html [text]
  (let [el (.createElement js/document "div")]
    (set! (.-innerText el) text)
    (.-innerHTML el)))

(defn inspect-prepl [value]
  (let [theme (theme/use-theme)
        opts  (ins/use-options)
        bg    (ins/get-background)
        search-text (ins/use-search-text)
        matcher     (f/match search-text)]
    [d/div
     {:style
      {:background    bg}}
     [title-bar-actions value]
     [d/div
      {:style
       {:border-left    [1 :solid (::c/border theme)]
        :border-right   [1 :solid (::c/border theme)]
        :border-bottom  [1 :solid (::c/border theme)]
        :border-bottom-left-radius (:border-radius theme)
        :border-bottom-right-radius (:border-radius theme)}}
      [:pre
       {:style
        {:margin         0
         :display        :flex
         :background     bg
         :max-height     (when-not (:expanded? opts) "24rem")
         :overflow       (when-not (:expanded? opts) :auto)
         :flex-direction :column-reverse
         :white-space    :pre-wrap
         :box-sizing     :border-box
         :padding        (:padding theme)
         :font-size      (:font-size theme)
         :font-family    (:font-family theme)}}
       [ins/with-collection
        value
        (reverse
         (keep-indexed
          (fn [index value]
            (when (matcher value)
              (with-meta
                (if (#{:tap :ret} (:tag value))
                  [ins/with-key
                   index
                   [inspect-prepl-ret value index]]
                  [d/span
                   {:style
                    {:color
                     (if (= (:tag value) :err)
                       (::c/exception theme)
                       (::c/text theme))}}
                   [h/html+ (anser/ansiToHtml
                             (escape-html (:val value))
                             #js {:use_classes true})]])
                {:key index})))
          value))]]]]))

(defn io? [value]
  (s/valid? ::prepl value))

(def viewer
  {:predicate io?
   :component #'inspect-prepl
   :name      :portal.viewer/prepl
   :doc       "View interlacing of stdout, stderr and tap values. Useful for build output."})
