(ns portal.runtime-test
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime :as rt]))

(deftest un-hashable-values
  (let [value   #?(:bb   :skip
                   :clj  (reify Object
                           (hashCode [_] (throw (Exception. "test"))))
                   :cljs (reify IHash
                           (-hash [_] (throw (js/Error. "test"))))
                   :default :skip)
        session {:id (atom 0)
                 :value-cache (atom {})}]
    (when-not (= :skip value)
      (binding [rt/*session* session]
        (is (= 1 (#'rt/value->id value)) "un-hashable values should produce a mapping")
        (is (= 1 (count @(:value-cache session))) "un-hashable values only capture one-way mapping")
        (is (= 2 (#'rt/value->id value)) "future captures introduce a new mapping")))))