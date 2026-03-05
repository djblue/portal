(ns portal.runtime-test
  (:require [clojure.test :refer [are deftest is]]
            #?(:clj [clojure.walk :as walk])
            [portal.runtime :as rt]
            #?(:clj [portal.runtime.cson :as cson])))

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

(deftest un-printable-values
  (let [value #?(:bb   :skip
                 :org.babashka/nbb :skip
                 :clj  (reify Object
                         (toString [_] (throw (Exception. "test"))))
                 :cljs (reify IPrintWithWriter
                         (-pr-writer [_ _ _] (throw (js/Error. "test"))))
                 :default :skip)]
    (when-not (= :skip value)
      (is (re-matches #"#object \[.* unprintable\]" (#'portal.runtime/pr-str' value))))))

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

    #{1 2 3} #?(:lpy :skip :default (sorted-set 1 2 3))

    ^{:one 1} [] ^{:two 2} []

    {:a ^{:one 2} [1]}
    {:a ^{:two 2} [1]}))

#?(:clj
   (deftype TestMap [^clojure.lang.IPersistentMap m ^clojure.lang.IPersistentMap _meta]
     clojure.lang.IPersistentMap
     (assoc [_ k v] (TestMap. (.assoc m k v) _meta))
     (assocEx [_ k v] (TestMap. (.assocEx m k v) _meta))
     (without [_ k] (TestMap. (.without m k) _meta))
     (cons [_ o] (TestMap. (.cons m o) _meta))
     (empty [_] (TestMap. (.empty m) _meta))
     (equiv [_ o] (and (instance? TestMap o) (.equiv m (.-m ^TestMap o))))

     clojure.lang.MapEquivalence

     clojure.lang.IObj
     (withMeta [_ new-meta] (TestMap. m new-meta))

     clojure.lang.IMeta
     (meta [_] _meta)

     clojure.lang.IHashEq
     (hasheq [_] (.hasheq ^clojure.lang.IHashEq m))

     clojure.lang.IKVReduce
     (kvreduce [_ f init] (.kvreduce ^clojure.lang.IKVReduce m f init))

     clojure.lang.ILookup
     (valAt [_ k] (.valAt m k))
     (valAt [_ k not-found] (.valAt m k not-found))

     clojure.lang.Seqable
     (seq [_] (.seq m))

     clojure.lang.Counted
     (count [_] (.count m))

     clojure.lang.Associative
     (containsKey [_ k] (.containsKey m k))
     (entryAt [_ k] (.entryAt m k))

     java.util.Map
     (size [_] (.size m))
     (isEmpty [_] (.isEmpty m))
     (containsValue [_ v] (.containsValue m v))
     (get [_ k] (.get m k))
     (keySet [_] (.keySet m))
     (values [_] (.values m))
     (entrySet [_] (.entrySet m))

     Iterable
     (iterator [_] (.iterator m))

     Object
     (hashCode [_] (.hashCode m))
     (equals [_ o] (and (instance? TestMap o) (.equals m (.-m ^TestMap o))))))

#?(:clj
   (deftype TestColl [^clojure.lang.IPersistentVector v ^clojure.lang.IPersistentMap _meta]
     clojure.lang.IPersistentCollection
     (cons [_ o] (TestColl. (.cons v o) _meta))
     (empty [_] (TestColl. (.empty v) _meta))
     (equiv [_ o] (and (instance? TestColl o) (.equiv v (.-v ^TestColl o))))
     (count [_] (.count v))

     clojure.lang.Sequential

     clojure.lang.IObj
     (withMeta [_ new-meta] (TestColl. v new-meta))

     clojure.lang.IMeta
     (meta [_] _meta)

     clojure.lang.IHashEq
     (hasheq [_] (.hasheq ^clojure.lang.IHashEq v))

     clojure.lang.IReduce
     (reduce [_ f] (reduce f v))
     (reduce [_ f init] (reduce f init v))

     clojure.lang.Seqable
     (seq [_] (.seq v))

     clojure.lang.Counted

     java.util.Collection
     (size [_] (.size v))
     (isEmpty [_] (.isEmpty v))
     (contains [_ o] (.contains v o))
     (toArray [_] (.toArray v))

     Iterable
     (iterator [_] (.iterator v))

     Object
     (hashCode [_] (.hashCode v))
     (equals [_ o] (and (instance? TestColl o) (.equals v (.-v ^TestColl o))))))

#?(:clj
   (defn- wire-refs-defined?
     "Write value via rt/write, then read the wire string back and verify
      every ref ID points to an ID that was defined in the same output."
     [value session]
     (let [wire       (binding [rt/*sent-values* (atom #{})]
                        (rt/write value session))
           ref-ids    (atom #{})
           parsed     (cson/read wire
                                 {:default-handler
                                  (fn [tag v]
                                    (when (= tag "ref")
                                      (swap! ref-ids conj v))
                                    (cson/tagged-value tag v))})
           defined-ids (atom #{})]
       (walk/postwalk
        (fn [node]
          (when-let [id (::rt/id (meta node))]
            (swap! defined-ids conj id))
          node)
        parsed)
       (and (seq @ref-ids)
            (every? @defined-ids @ref-ids)))))

(deftest java-util-map-no-dual-cache-ids
  (let [value #?(:clj (->TestMap {:a 1} nil) :default :skip)
        session {:id (atom 0) :value-cache (atom {})}]
    (when-not (= :skip value)
      (binding [rt/*session* session]
        (is (wire-refs-defined? [value value] session)
            "every ref should point to an id defined in the same wire output")))))

(deftest java-util-collection-no-dual-cache-ids
  (let [value #?(:clj (->TestColl [1 2 3] nil) :default :skip)
        session {:id (atom 0) :value-cache (atom {})}]
    (when-not (= :skip value)
      (binding [rt/*session* session]
        (is (wire-refs-defined? [value value] session)
            "every ref should point to an id defined in the same wire output")))))
