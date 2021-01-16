(ns portal.ui.viewer.diff
  (:require [cognitect.transit :as t]
            [lambdaisland.deep-diff2 :refer [diff]]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.ui.inspector :as ins :refer [inspector]]))

(def readers
  {"portal.transit/Deletion"  (t/read-handler #(diff/Deletion. %))
   "portal.transit/Insertion" (t/read-handler #(diff/Insertion. %))
   "portal.transit/Mismatch"  (t/read-handler #(let [[a b] %] (diff/Mismatch. a b)))})

(def writers
  {diff/Deletion  (t/write-handler (constantly "portal.transit/Deletion") :-)
   diff/Insertion (t/write-handler (constantly "portal.transit/Insertion") :+)
   diff/Mismatch  (t/write-handler (constantly "portal.transit/Mismatch") (juxt :- :+))})

(defn diff? [value]
  (or
   (instance? diff/Deletion value)
   (instance? diff/Insertion value)
   (instance? diff/Mismatch value)))

(defn can-view? [value]
  (or (diff? value) (and (vector? value) (= 2 (count value)))))

(defn inspect-diff [settings value]
  (cond
    (instance? diff/Deletion value)  [inspector settings (:- value)]
    (instance? diff/Insertion value) [inspector settings (:+ value)]
    (instance? diff/Mismatch value)  [inspector settings value]
    :else
    (let [[a b] value]
      [inspector (assoc settings :depth 0) (diff a b)])))

(def viewer
  {:predicate can-view?
   :component inspect-diff
   :datafy diff
   :name :portal.viewer/diff})
