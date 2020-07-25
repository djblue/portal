(ns portal.viewer.diff
  (:require [lambdaisland.deep-diff2 :refer [diff]]
            [portal.inspector :as ins :refer [inspector]]))

(defn can-view? [value]
  (and (vector? value) (= 2 (count value))))

(defn inspect-diff [settings value]
  (let [[a b] value]
    [inspector settings (diff a b)]))
