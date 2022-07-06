(ns portal.ui.viewer.log
  (:require [clojure.spec.alpha :as sp]
            [portal.colors :as c]
            [portal.resources :refer [inline]]
            #_[shadow.resource :refer [inline]] ;; for hot reloading
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.date-time :as date-time]))

(defn- parse [xml-string]
  (let [parser (js/DOMParser.)
        dom    (.parseFromString parser xml-string "text/xml")]
    (aget (.getElementsByTagName dom "svg") 0)))

(defn- stringify [dom]
  (.serializeToString (js/XMLSerializer.) dom))

(defn- resolve-color [color]
  (if-let [[_ var] (re-matches #"var\((.*)\)" color)]
    (-> js/document
        .-documentElement
        js/getComputedStyle
        (.getPropertyValue var))
    color))

(defn- theme-svg [svg color]
  (let [color (resolve-color color)]
    (doseq [el (.querySelectorAll svg "[fill]")]
      (.setAttribute el "fill" color))
    (doseq [el (.querySelectorAll svg "[stroke]")]
      (.setAttribute el "stroke" color)))
  svg)

(def runtime->logo
  {:clj    (inline "runtime/clojure.svg")
   :cljs   (inline "runtime/cljs.svg")
   :bb     (inline "runtime/babashka.svg")
   :portal (inline "runtime/portal.svg")})

(defn icon [value color]
  [s/img
   {:style
    {:height 22 :width 22}
    :src (str
          "data:image/svg+xml;base64,"
          (-> value runtime->logo parse (theme-svg color) stringify js/btoa))}])

(def ^:private levels
  [:trace :debug :info :warn :error :fatal :report])

(sp/def ::level (set levels))

(sp/def ::ns symbol?)
(sp/def ::time ins/date?)

(sp/def ::column int?)
(sp/def ::line int?)

(sp/def ::log
  (sp/keys :req-un
           [::level
            ::ns
            ::time
            ::line
            ::column]))

(defn log? [value]
  (sp/valid? ::log value))

(def ^:private level->color
  {:trace  ::c/text
   :debug  ::c/string
   :info   ::c/boolean
   :warn   ::c/tag
   :error  ::c/exception
   :fatal  ::c/exception
   :report ::c/border})

(defn inspect-log [log]
  (let [theme      (theme/use-theme)
        background (ins/get-background)
        color      (-> log :level level->color theme)
        runtime    (:runtime log)
        runtime?   (contains? runtime->logo runtime)
        options    (ins/use-options)
        expanded?  (:expanded? options)
        border     (when-not expanded?
                     {:border-bottom [1 :solid (::c/border theme)]})
        flex       {:box-sizing  :border-box
                    :padding     (:padding theme)
                    :display     :flex
                    :align-items :center}]

    [s/div
     {:style
      {:background background}}
     [s/div
      {:style
       {:display                   :grid
        :grid-template-columns     (if-not runtime?
                                     "auto 1fr auto"
                                     "auto 1fr auto auto")
        :border-left               [5 :solid color]
        :border-top-left-radius    (:border-radius theme)
        :border-bottom-left-radius (when-not expanded? (:border-radius theme))}}
      [s/div
       {:style
        (merge {:border-top [1 :solid (::c/border theme)]} flex border)}
       [date-time/inspect-time (:time log)]]
      [s/div
       {:style
        (merge flex {:border-top [1 :solid (::c/border theme)] :flex "1"} border)}
       [select/with-position
        {:row -1 :column 0}
        [ins/inspector (:result log)]]]
      [s/div
       {:style
        (merge
         flex
         {:color           (::c/uri theme)
          :border-top      [1 :solid (::c/border theme)]
          :justify-content :flex-end}
         border
         (when-not runtime?
           {:border-right               [1 :solid (::c/border theme)]
            :border-top-right-radius    (:border-radius theme)
            :border-bottom-right-radius (:border-radius theme)}))}
       (:ns log)
       ":"
       (:line log)]
      (when runtime?
        [s/div
         {:style
          (merge
           {:padding                    (:padding theme)
            :display                    :flex
            :align-items                :center
            :color                      (::c/uri theme)
            :border-top                 [1 :solid (::c/border theme)]
            :border-left                [1 :solid (::c/border theme)]
            :border-right               [1 :solid (::c/border theme)]
            :border-top-right-radius    (:border-radius theme)
            :border-bottom-right-radius (when-not expanded? (:border-radius theme))}
           border)}
         [icon runtime (::c/package theme)]])]

     (when (:expanded? options)
       [ins/inspect-map-k-v (dissoc log :level :result :line :column :ns :runtime)])]))

(def viewer
  {:predicate log?
   :component inspect-log
   :name      :portal.viewer/log})
