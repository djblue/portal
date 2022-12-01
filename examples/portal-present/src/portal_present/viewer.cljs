(ns portal-present.viewer
  (:require [portal.colors :as c]
            [portal.ui.api :as p]
            [portal.ui.commands :as commands]
            [portal.ui.icons :as icons]
            [portal.ui.inspector :as ins]
            [portal.ui.select :as select]
            [portal.ui.state :as state]
            [portal.ui.styled :as d]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(commands/toggle-shell state/state)

(defn- button [{:keys [icon on-click]}]
  (let [theme (theme/use-theme)]
    [d/div
     {:style
      {:cursor :pointer
       :padding (:padding theme)}
      :style/hover {:color (::c/tag theme)}
      :on-click (fn [e]
                  (.stopPropagation e)
                  (on-click))}
     [icon]]))

(defn view-presentation []
  (let [slide (r/atom 0)]
    (fn [slides]
      (let [theme              (theme/use-theme)
            background         (ins/get-background)]
        [d/div
         {:style
          {:background background
           :padding (:padding theme)
           :border-radius (:border-radius theme)
           :border [1 :solid (::c/border theme)]}}
         [d/div
          {:style
           {:padding-top 20
            :min-height "50vh"}}
          [select/with-position
           {:row 0 :column 0}
           [ins/with-key
            @slide
            [ins/dec-depth
             [ins/inspector (nth (seq slides) @slide :no-slide)]]]]]
         [d/div
          {:style
           {:display :flex
            :align-items :center
            :justify-content :space-between}}
          [d/div
           "Slide "
           [d/span {:style {:color (::c/number theme)}} (inc @slide)]
           " of "
           [d/span {:style {:color (::c/number theme)}} (count slides)]]
          [d/div
           {:style {:display :flex}}
           [button
            {:icon icons/arrow-left
             :on-click #(when (> @slide 0)
                          (swap! slide dec))}]
           [button
            {:icon icons/arrow-right
             :on-click #(when (< (inc @slide) (count slides))
                          (swap! slide inc))}]]]]))))

(defn deck? [value] (and (coll? value) (not (map? value))))

(p/register-viewer!
 {:name ::slides
  :predicate deck?
  :component view-presentation})
