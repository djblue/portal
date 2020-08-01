(ns portal.drag-and-drop
  (:require [portal.styled :as s]
            [reagent.core :as r]))

(defn file->map [file]
  {:name (.-name file)
   :size (.-size file)
   :type (.-type file)
   :last-modified (js/Date. (.-lastModified file))
   :text          (.text file)})

(defn area []
  (let [active? (r/atom false)]
    (fn [settings children]
      [s/div
       {:on-drag-over
        (fn [e]
          (.preventDefault e)
          (reset! active? true))
        :on-drag-leave
        (fn [_e]
          (reset! active? false))
        :on-drop
        (fn [e]
          (.preventDefault e)
          (let [value (for [item (.-dataTransfer.items e)
                            :when (= (.-kind item) "file")]
                        (.getAsFile item))]
            ((:set-settings! settings)
             {:portal/value (mapv file->map value)}))
          (reset! active? false))
        :style {:position :relative}}
       (when @active?
         [s/div
          {:style
           {:position :absolute
            :top 0
            :left 0
            :right 0
            :bottom 0
            :z-index 100
            :padding 40
            :box-sizing :border-box
            :background "rgba(0,0,0,0.5)"
            :color "white"}}
          [s/div
           {:style
            {:border "5px dashed white"
             :border-radius 20
             :height "calc(100% - 10px)"
             :display :flex
             :justify-content :center
             :align-items :center}}
           [:h1 {:style {:font-family "sans-serif"}} "Drag & Drop Your File Here"]]])
       children])))
