(ns portal.runtime.cson-test
  (:require [clojure.test :refer [deftest are is]]
            [portal.runtime.cson :as cson])
  #?(:clj  (:import [java.util Date]
                    [java.util UUID])
     :cljr (:import [System DateTime Guid]
                    [System.Text Encoding])))

(defn pass [v]
  (cson/read (cson/write v)))

(deftest simple-values
  (are [value]
       (= value (pass value))
    nil
    0
    1.0
    1.5
    #?(:clj  42N
       :cljr 42N
       :joyride (js/BigInt "42")
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
    (range 10)
    (first {:a 1})))

(deftest range-with-meta
  (let [v (with-meta (range 0 5 1.0) {:my :meta})]
    #?(:clj  (is (= '() (pass v)) "Range with meta doesn't work in clj")
       :cljr (is (= v (pass v))  "Range with meta works in cljr")
       :cljs (is (= v (pass v))  "Range with meta works in cljs")))
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

#?(:lpy nil
   :default
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
       (is (= (seq a) (seq b))))))

(def tagged
  [#?(:clj  (Date.)
      :cljr (DateTime/Now)
      :cljs (js/Date.))
   #?(:clj  (UUID/randomUUID)
      :cljr (Guid/NewGuid)
      :cljs (random-uuid))
   #?(:lpy nil
      :default (tagged-literal 'tag :value))])

(deftest tagged-objects
  (doseq [value tagged]
    (is (= (pass value) (pass value)))))

(defn meta* [v]
  #?(:bb (dissoc (meta v) :type) :cljr (meta v) :clj (meta v) :cljs (meta v)))

(deftest tagged-values []
  (let [v1 (cson/tagged-value "my/tag" {:hello :world})
        v2 (with-meta v1 {:my :meta})]
    (are [v] (= v (pass v)) v1 v2)
    (are [v] (= (meta* v) (meta* (pass v))) v1 v2))
  (is (thrown?
       #?(:clj AssertionError :cljr Exception :cljs js/Error)
       (cson/tagged-value :my/tag {:hello :world}))
      "only allow string tags"))

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
     (is (= 4611681620380904123 (pass 4611681620380904123))))
   :cljr
   (deftest clr-longs
     (is (= 1 (byte 1)  (pass (byte 1))))
     (is (= 1 (short 1) (pass (short 1))))
     (is (= 1 (int 1)   (pass (int 1))))
     (is (= 1 (long 1)  (pass (long 1))))
     (is (= 4611681620380904123 (pass 4611681620380904123)))))

#?(:clj
   (deftest java-chars
     (is (= \A (pass \A)))
     (is (= (seq "hi") (pass (seq "hi")))))
   :joyride nil
   :org.babashka/nbb nil
   :cljs
   (deftest js-chars
     (let [a (cson/Character. 10) b (cson/Character. 10)]
       (is (= a b))
       (is (= a (pass b))))))

(deftest special-numbers
  (doseq [n    [##NaN ##Inf ##-Inf]
          :let [cson (cson/write n)]]
    (is (= cson (cson/write (cson/read cson))) n)))

(deftest binary
  (let [bin #?(:clj  (.getBytes "hi")
               :cljr (.GetBytes Encoding/UTF8 "hi")
               :cljs (.encode (js/TextEncoder.) "hi")
               :default nil)]
    (when bin
      (is (= "[\"bin\",\"aGk=\"]" (cson/write bin))))))