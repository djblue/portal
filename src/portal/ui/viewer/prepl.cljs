(ns portal.ui.viewer.prepl
  (:require ["anser" :as anser]
            [clojure.spec.alpha :as s]
            [portal.colors :as c]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]))

;;; :spec
(s/def ::tag #{:out :err :tap})

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

(defn inspect-prepl [value]
  (let [theme (theme/use-theme)
        opts  (ins/use-options)]
    [d/div
     {:style
      {:background    (ins/get-background)
       :border-radius (:border-radius theme)
       :border        [1 :solid (::c/border theme)]}}
     [d/div
      {:style
       {:display       :flex
        :gap           (:padding theme)
        :box-sizing    :border-box
        :padding       (* 1.6 (:padding theme))
        :border-bottom [1 :solid (::c/border theme)]}}
      [icons/circle {:size :xs :style {:color (::c/exception theme)}}]
      [icons/circle {:size :xs :style {:color (::c/tag theme)}}]
      [icons/circle {:size :xs :style {:color (::c/string theme)}}]]
     [:pre
      {:style
       {:margin         0
        :display        :flex
        :max-height     (when-not (:expanded? opts) "24rem")
        :overflow       :auto
        :flex-direction :column-reverse
        :white-space    :pre-wrap
        :box-sizing     :border-box
        :padding        (:padding theme)
        :font-size      (:font-size theme)
        :font-family    (:font-family theme)}}
      (reverse
       (map-indexed
        (fn [index value]
          (with-meta
            (if (= (:tag value) :tap)
              [ins/with-collection
               value
               [ins/with-key
                index
                [select/with-position
                 {:row index :column 0}
                 [d/div
                  {:style {:padding
                           [(* 2 (:padding theme)) (:padding theme)]}}
                  [ins/inspector
                   (try
                     (ins/read-string (:val value))
                     (catch :default _ (:val value)))]]]]]
              [d/span
               {:style
                {:color
                 (if (= (:tag value) :err)
                   (::c/exception theme)
                   (::c/text theme))}
                :dangerouslySetInnerHTML
                {:__html (anser/ansiToHtml (:val value) #js {:use_classes true})}}])
            {:key index}))
        value))]]))

(defn io? [value]
  (s/valid? ::prepl value))

(def viewer
  {:predicate io?
   :component inspect-prepl
   :name      :portal.viewer/prepl})
