(ns portal.ui.viewer.diff
  (:require [lambdaisland.deep-diff2 :refer [diff]]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.ui.inspector :as ins]))

(defn diff? [value]
  (or
   (instance? diff/Deletion value)
   (instance? diff/Insertion value)
   (instance? diff/Mismatch value)))

(defn can-view? [value]
  (or (diff? value) (and (vector? value) (= 2 (count value)))))

(defn inspect-diff [value]
  (cond
    (instance? diff/Deletion value)  [ins/inspector (:- value)]
    (instance? diff/Insertion value) [ins/inspector (:+ value)]
    (instance? diff/Mismatch value)  [ins/inspector value]
    :else
    (let [[a b] value]
      [ins/inspector (diff a b)])))

(def viewer
  {:predicate can-view?
   :component inspect-diff
   :name :portal.viewer/diff})
