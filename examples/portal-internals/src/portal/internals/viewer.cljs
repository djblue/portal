(ns portal.internals.viewer
  (:require [portal.colors :as c]
            [portal.ui.api :as p]
            [portal.ui.commands :as commands]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as d]
            [portal.ui.rpc :as rpc]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(defn- button [{:keys [icon on-click]}]
  (let [theme (theme/use-theme)]
    [d/div
     {:style
      {:cursor :pointer
       :box-sizing :border-box
       :padding (:padding theme)}
      :style/hover {:color (::c/tag theme)}
      :on-click (fn [e]
                  (.stopPropagation e)
                  (on-click))}
     [icon]]))

(defn view-presentation [{:keys [slides portals current-slide] :as data}]
  (let [theme      (theme/use-theme)
        context    (ins/use-context)
        parent     (:value (:parent context))
        background (ins/get-background)]
    [d/div
     {:style {:display :flex
              :gap 10
              :min-height "calc(100vh - 28px)"}}
     [d/div
      {:style
       {:min-width 960
        :font-size 24
        :display :flex
        :justify-content :space-between
        :flex-direction :column
        :background background
        :box-sizing :border-box
        :padding (* 5 (:padding theme))
        :border-radius (:border-radius theme)
        :border [1 :solid (::c/border theme)]}}
      [d/div
       {:style
        {:padding-top 20}}
       [select/with-position
        {:row 0 :column 0}
        [ins/with-key
         current-slide
         [ins/dec-depth
          [ins/inspector (:slide (nth (seq slides) current-slide) :no-slide)]]]]]
      [d/div
       {:style
        {:display :flex
         :align-items :center
         :justify-content :space-between}}
       [d/div
        "Slide "
        [d/span {:style {:color (::c/number theme)}} (inc current-slide)]
        " of "
        [d/span {:style {:color (::c/number theme)}} (count slides)]]
       [d/div
        {:style {:display :flex}}
        [button
         {:icon icons/arrow-left
          :on-click #(rpc/call `prev-slide parent)}]
        [button
         {:icon icons/arrow-right
          :on-click #(rpc/call `next-slide parent)}]]]]
     (into
      [d/div
       {:style {:flex "1"
                :gap 10
                :min-height "100%"
                :display :flex
                :flex-direction :column}}]
      @portals)]))

(defn deck? [value] (map? value))

(p/register-viewer!
 {:name ::slides
  :predicate deck?
  :component view-presentation})
