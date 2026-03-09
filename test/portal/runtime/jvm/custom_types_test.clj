(ns portal.runtime.jvm.custom-types-test
  (:refer-clojure :exclude [read])
  (:require
   [clojure.test :refer [deftest is]]
   [portal.runtime.cson :as cson]
   [portal.runtime :as rt]))

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
  (equals [_ o] (and (instance? TestMap o) (.equals m (.-m ^TestMap o)))))

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
  (equals [_ o] (and (instance? TestColl o) (.equals v (.-v ^TestColl o)))))

(defn- open-session []
  (rt/open-session {:session-id (random-uuid)}))

(defn- write [value session]
  (binding [rt/*sent-values* (:sent-values session)]
    (rt/write value session)))

(defn- read [string session]
  (rt/read string session))

(defn- pass
  ([value]
   (pass value (open-session)))
  ([value session]
   (let [cson (write value session)]
     {:value (cson/read
              cson
              {:default-handler
               (fn [op value]
                 (cson/tagged-value op value))})
      :session session})))

(deftest custom-coll-type-serialization
  (let [test-coll (TestColl. [1 2 3] {})
        {:keys [session] [a b] :value} (pass [test-coll test-coll])
        id (::rt/id (meta a))]
    (is (coll? a))
    (is (= a test-coll))
    (is (not (identical? a test-coll)))
    (is (some? id))
    (is (== id (:rep b)))
    (is (contains? @(:value-cache session) [:id id]))
    (is (contains? @(:sent-values session) test-coll))))

(deftest custom-map-type-serialization
  (let [test-map (TestMap. {:hello :world} {})
        {:keys [session] [a b] :value} (pass [test-map test-map])
        id (::rt/id (meta a))]
    (is (map? a))
    (is (= a test-map))
    (is (not (identical? a test-map)))
    (is (some? id))
    (is (== id (:rep b)))
    (is (contains? @(:value-cache session) [:id id]))
    (is (contains? @(:sent-values session) test-map))))