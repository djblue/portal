(ns portal.ui.viewer.diff
  (:require [lambdaisland.deep-diff2 :refer [diff]]
            [lambdaisland.deep-diff2.diff-impl :as diff]
            [portal.runtime.cson :as cson]
            [portal.ui.commands :as commands]
            [portal.ui.inspector :as ins]))

(extend-protocol cson/ToJson
  diff/Deletion
  (-to-json [this buffer] (cson/tag buffer "diff/Deletion" (:- this)))

  diff/Insertion
  (-to-json [this buffer] (cson/tag buffer "diff/Insertion" (:+ this)))

  diff/Mismatch
  (-to-json [this buffer] (cson/tag buffer "diff/Mismatch" ((juxt :- :+) this))))

(defn ^:no-doc ->diff [op value]
  (case op
    "diff/Deletion"  (diff/Deletion.  value)
    "diff/Insertion" (diff/Insertion. value)
    "diff/Mismatch"  (let [[a b] value]
                       (diff/Mismatch. a b))
    nil))

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
