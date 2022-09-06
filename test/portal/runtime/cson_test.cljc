(ns portal.runtime.cson-test
  (:require [clojure.test :refer [deftest are is]]
            [portal.runtime.cson :as cson])
  #?(:clj (:import [java.util Date]
                   [java.util UUID])))

(defn pass [v]
  (cson/read (cson/write v)))

(deftest simple-values
  (are [value]
       (= value (pass value))
    nil
    0
    1.0
    #?(:clj 42N
       :cljs (when (exists? js/BigInt)
               (js/BigInt "42")))
    \newline
    true
    false
    'hello
    'hello/world
    :hello
    :hello/world
    ""
    "hello"
    "hello/world"))

(deftest escape-strings
  (are [value]
       (= value (pass value))
    "\n"
    "\""
    " \"hello\" "))

(deftest basic-collections
  (are [value]
       (= value (pass value))
    []
    [1 2 3]
    {}
    {:a :b}
    #{}
    #{1 2 3}
    '()
    (list 1 2 3)))

(def composite-value
  ['hello
   'hello/world
   '(1 2 3)
   ""
   3.14
   true
   false
   #inst "2021-04-07T22:43:59.393-00:00"
   #uuid "1d80bdbb-ab16-47b2-a8bd-068f94950248"
   nil
   1
   \h
   "data"
   {:hello/world :grep}
   #{1 2 3}])

(deftest composite-collections
  (are [value]
       (= value (pass value))
    [[[]]]
    #{#{#{}}}
    {{} {}}
    {[] []}
    {#{} #{}}
    {(list) (list)}
    (list [] #{} {})
    composite-value))

(deftest special-collections
  (are [value]
       (= value (pass value))
    (range 10)))

(deftest range-with-meta
  (let [v (with-meta (range 0 5 1.0) {:my :meta})]
    #?(:clj  (is (= '() (pass v)) "Range with meta doesn't work in clj")
       :cljs (is (= v (pass v))  "Range with meta doesn't work in cljs")))
  (let [v (range 0 5 1.0)]
    (is (= v (pass v)) "Range with no meta works correctly"))
  (let [v (with-meta (range 0 5 1) {:my :meta})]
    (is (= v (pass v)) "LongRange with no meta works correctly")))

(deftest seq-collections
  (are [value]
       (= (seq value) (pass (seq value)))
    '(0)
    [0]
    #{0}
    {0 0}))

(deftest sorted-collections
  (let [a (sorted-map :a 1 :c 3 :b 2)
        b (pass a)]
    (is (= a b))
    (is (= (keys a) (keys b)))
    (is (= (type a) (type b))))
  (let [a (sorted-map-by > 1 "a" 2 "b" 3 "c")
        b (pass a)]
    (is (= a b))
    (is (= (keys a) (keys b)))
    (is (= (type a) (type b))))
  (let [a (sorted-set 1 2 3)
        b (pass a)]
    (is (= a b))
    (is (= (seq a) (seq b))))
  (let [a (sorted-set-by > 1 2 3)
        b (pass a)]
    (is (= a b))
    (is (= (seq a) (seq b)))))

(def tagged
  [#?(:clj  (Date.)
      :cljs (js/Date.))
   #?(:clj  (UUID/randomUUID)
      :cljs (random-uuid))
   (tagged-literal 'tag :value)])

(deftest tagged-objects
  (doseq [value tagged]
    (is (= value (pass value)))))

(deftest metadata
  (doseq [value ['hello {} [] #{}]]
    (let [m     {:my :meta}
          value (with-meta value m)]
      (is (= m (meta (pass value)))))))

(deftest symbol-key-with-meta
  (let [m     {:a :b}
        value {(with-meta 'k m) 'v}]
    (is (= value (pass value)))
    (is (= m (meta (first (keys (pass value))))))))

#?(:clj
   (deftest java-longs
     (is (= 1 (byte 1)  (pass (byte 1))))
     (is (= 1 (short 1) (pass (short 1))))
     (is (= 1 (int 1)   (pass (int 1))))
     (is (= 1 (long 1)  (pass (long 1))))
     (is (= 4611681620380904123 (pass 4611681620380904123)))))
