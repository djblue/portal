(ns portal.styled
  (:require [reagent.core :as r]
            ["emotion" :as emotion]))

(def selectors
  {:style       identity
   :style/hover (fn [style] {:&:hover style})})

(defn attrs->css [attrs]
  (reduce
   (fn [attrs selector]
     (if-not (contains? attrs selector)
       attrs
       (let [style  (get attrs selector)
             f      (get selectors selector)
             class  (-> style f clj->js emotion/css)]
         (-> attrs
             (dissoc selector)
             (update :class emotion/cx class)))))
   attrs
   (keys selectors)))

(defn styled [component attrs & children]
  (into [component
         (if-not (map? attrs)
           attrs
           (attrs->css attrs))]
        children))

(def a      (partial styled :a))
(def table  (partial styled :table))
(def tbody  (partial styled :tbody))
(def thead  (partial styled :thead))
(def tr     (partial styled :tr))
(def th     (partial styled :th))
(def td     (partial styled :td))
(def div    (partial styled :div))
(def span   (partial styled :span))
(def input  (partial styled :input))
(def button (partial styled :button))

