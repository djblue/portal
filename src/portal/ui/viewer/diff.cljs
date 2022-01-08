(ns portal.ui.viewer.diff
  (:require [lambdaisland.deep-diff2 :refer [diff]]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.runtime.cson :as cson]
            [portal.ui.commands :as commands]
            [portal.ui.inspector :as ins]))

(defn- to-json [tag value]
  (cson/tag tag (cson/to-json value)))

(extend-protocol cson/ToJson
  diff/Deletion
  (-to-json [this] (to-json "diff/Deletion" (:- this)))

  diff/Insertion
  (-to-json [this] (to-json "diff/Insertion" (:+ this)))

  diff/Mismatch
  (-to-json [this] (to-json "diff/Mismatch" ((juxt :- :+) this))))

(defn ^:no-doc diff-> [value]
  (case (first value)
    "diff/Deletion"  (diff/Deletion.  (cson/json-> (second value)))
    "diff/Insertion" (diff/Insertion. (cson/json-> (second value)))
    "diff/Mismatch"  (let [[a b] (cson/json-> (second value))]
                       (diff/Mismatch. a b))))

(defn diff? [value]
  (or
   (instance? diff/Deletion value)
   (instance? diff/Insertion value)
   (instance? diff/Mismatch value)))

(defn- can-view? [value]
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

(let [var  #'diff
      name (#'commands/var->name var)]
  (swap! commands/registry
         assoc name (commands/make-command
                     (merge (meta var)
                            {:predicate (fn [& args] (= 2 (count args)))
                             :f var
                             :name name}))))
