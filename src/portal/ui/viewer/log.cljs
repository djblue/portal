(ns portal.ui.viewer.log
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.resources :refer [inline]]
            #_[shadow.resource :refer [inline]] ;; for hot reloading
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
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

(def ^:private runtime->logo
  {:clj    {:icon (inline "runtime/clojure.svg")  :color ::c/package}
   :cljr   {:icon (inline "runtime/clojure.svg")  :color ::c/string}
   :cljs   {:icon (inline "runtime/cljs.svg")     :color ::c/tag}
   :bb     {:icon (inline "runtime/babashka.svg") :color ::c/exception}
   :portal {:icon (inline "runtime/portal.svg")   :color ::c/boolean}})

(defn icon [value color]
  [d/img
   {:style
    {:height 22 :width 22}
    :src (str
          "data:image/svg+xml;base64,"
          (-> value runtime->logo :icon parse (theme-svg color) stringify js/btoa))}])

;;; :spec
(def ^:private levels
  [:trace :debug :info :warn :error :fatal :report])

(s/def ::level (set levels))

(s/def ::ns symbol?)
(s/def ::time inst?)

(s/def ::column int?)
(s/def ::line int?)

(s/def ::log
  (s/keys :req-un
          [::level
           ::ns
           ::time
           ::line
           ::column]))
;;;

(defn log? [value]
  (s/valid? ::log value))

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
        border     (cond-> {:border-top [1 :solid (::c/border theme)]}
                     (not expanded?)
                     (assoc
                      :border-bottom [1 :solid (::c/border theme)]))
        flex       {:box-sizing  :border-box
                    :padding     (:padding theme)
                    :display     :flex
                    :align-items :center}]

    [d/div
     {:style
      {:background background}}
     [d/div
      {:style
       {:display                   :grid
        :grid-template-columns     (if-not runtime?
                                     "auto auto 1fr auto"
                                     "auto auto 1fr auto auto")
        :border-left               [5 :solid color]
        :border-top-left-radius    (:border-radius theme)
        :border-bottom-left-radius (when-not expanded? (:border-radius theme))}}
      [ins/toggle-expand
       {:style
        (merge {:padding-left (:padding theme)} border)}]
      [d/div
       {:style (merge flex border)}
       [date-time/inspect-time (:time log)]]
      [d/div
       {:style
        (merge flex {:border-top [1 :solid (::c/border theme)] :flex "1"} border)}
       [select/with-position
        {:row -1 :column 0}
        [ins/with-collection log
         [ins/with-key :result
          [ins/dec-depth
           [ins/inspector (:result log)]]]]]]
      [d/div
       {:on-click
        (fn [e]
          (.stopPropagation e)
          (rpc/call 'portal.runtime.jvm.editor/goto-definition log))
        :style/hover
        {:opacity 1
         :text-decoration :underline}
        :style
        (merge
         flex
         {:opacity         0.75
          :cursor          :pointer
          :color           (::c/uri theme)
          :border-top      [1 :solid (::c/border theme)]
          :justify-content :flex-end}
         border
         (when-not runtime?
           {:border-right               [1 :solid (::c/border theme)]
            :border-top-right-radius    (:border-radius theme)
            :border-bottom-right-radius (:border-radius theme)}))}
       [ins/highlight-words (str (:ns log) ":" (:line log))]]
      (when runtime?
        [d/div
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
         [icon
          runtime
          (get theme (get-in runtime->logo [runtime :color] ::c/text))]])]

     (when (:expanded? options)
       [ins/with-collection
        log
        [ins/inspect-map-k-v (dissoc log :level :result :line :column :ns :runtime)]])]))

(def viewer
  {:predicate log?
   :component inspect-log
   :name      :portal.viewer/log
   :doc       "Useful for conveying a value in a specific context (what/where/when)."})
