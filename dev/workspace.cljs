(ns workspace
  (:require ["react" :as react]
            ["vega-embed" :as vega-embed]
            [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]
            [reagent.core :as r]))

(def counter (r/atom 0))

(defn button [props & children]
  (let [theme (theme/use-theme)]
    (into
     [s/button
      (merge
       {:style
        {:border :none
         :cursor :pointer
         :padding (:padding theme)
         :font-size (:font-size theme)
         :border-radius (:border-radius theme)
         :background (::c/boolean theme)
         :font-family (:font-family theme)
         :color (::c/text theme)}}
       props)]
     children)))

(defn hello-world []
  (let [theme              (theme/use-theme)
        [state set-state!] (react/useState 0)]
    (js/console.log vega-embed/default)
    [s/div
     {:style
      {:border-top [1 :solid (::c/border theme)]
       :padding (* 2 (:padding theme))}}
     [s/h1 {:style
            {:display :flex
             :color (::c/boolean theme)}}
      "Counter: "
      [ins/inspector @counter]
      [ins/inspector state]]
     [button {:on-click
              (fn []
                (set-state! (inc state))
                (tap> {:action  :inc
                       :current @counter
                       :next    (swap! counter inc)}))}
      "Click me!"]]))
