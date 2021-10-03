(ns portal.ui.viewer.log
  (:require [clojure.spec.alpha :as sp]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [portal.ui.viewer.date-time :as date-time]))

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

(defn inspect-log [log]
  (let [theme      (theme/use-theme)
        background (ins/get-background)]
    [s/div
     {:style
      {:display :grid
       :grid-template-columns "auto 1fr auto"}}
     [s/div
      {:style
       {:box-sizing :border-box
        :padding (:padding theme)
        :background background
        :border-left [1 :solid (::c/border theme)]
        :border-top [1 :solid (::c/border theme)]
        :border-bottom [1 :solid (::c/border theme)]}}
      [date-time/inspect-time (:time log)]]
     [s/div
      {:style
       {:background background
        :padding (:padding theme)
        :border-top [1 :solid (::c/border theme)]
        :border-bottom [1 :solid (::c/border theme)]}}
      [select/with-position
       {:row 0 :column 0}
       [ins/inspector (:result log)]]]
     [s/div
      {:style
       {:background background
        :padding (:padding theme)
        :color (::c/uri theme)
        :border-top [1 :solid (::c/border theme)]
        :border-bottom [1 :solid (::c/border theme)]
        :border-right [1 :solid (::c/border theme)]
        :text-align :right}}
      (:ns log)
      ":"
      (:line log)]]))

(def viewer
  {:predicate log?
   :component inspect-log
   :name      :portal.viewer/log})
