(ns portal.ui.viewer.deref
  (:require [portal.colors :as c]
            [portal.ui.inspector :as ins]
            [portal.ui.rpc :as rpc]
            [portal.ui.select :as select]
            [portal.ui.styled :as s]
            [portal.ui.theme :as theme]))

(defn atom? [value]
  (and (rpc/runtime-object? value)
       (not= (rpc/tag value) :var)
       (rpc/-satisfies? value :IDeref)))

(defn inspect-deref [value]
  (let [theme (theme/use-theme)
        value @value]
    [s/div
     {:style
      {:position :relative}}
     [s/div
      {:style
       {:top        0
        :right      0
        :position   :absolute
        :cursor     :pointer
        :color      (::c/tag theme)
        :font-size  (:font-size theme)
        :box-sizing :border-box
        :padding    (if-not (coll? value)
                      0
                      (inc (:padding theme)))}
       :style/hover
       {:color (::c/namespace theme)}}
      "@"]
     [select/with-position
      {:row 0 :column 0}
      [ins/dec-depth [ins/inspector value]]]]))

(def viewer
  {:predicate atom?
   :component inspect-deref
   :name :portal.viewer/deref})
