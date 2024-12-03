(ns portal.runtime-test
  (:require
   [clojure.test :refer [are deftest is]]
   [portal.runtime :as rt]))

(deftest un-hashable-values
  (let [value   #?(:bb   :skip
                   :org.babashka/nbb :skip
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

(deftest disambiguate-types
  (are [a b]
       (= (#'rt/value->key a) (#'rt/value->key b))

       [] []

       [1] [1]

       ^:one [1] ^:one [1]

       {:a ^:one [1]}
       {:a ^:one [1]})

  (are [a b]
       (not= (#'rt/value->key a) (#'rt/value->key b))

       [1] '(1)

       #{1 2 3} (sorted-set 1 2 3)

       ^{:one 1} [] ^{:two 2} []

       {:a ^{:one 2} [1]}
       {:a ^{:two 2} [1]}))
