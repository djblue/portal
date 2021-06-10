(ns portal.runtime.cson-test
  #?(:clj
     (:require [clojure.test :refer [deftest are is]]
               [cognitect.transit :as transit]
               [portal.runtime.cson :as cson]
               [cheshire.core :as json])
     :cljs
     (:require [clojure.test :refer [deftest are is]]
               [cognitect.transit :as transit]
               [portal.runtime.cson :as cson])
     :clje
     (:require [clojure.test :refer [deftest are is]]
               [portal.runtime.cson :as cson]
               [jiffy]))

  #?(:clj (:import [java.io ByteArrayOutputStream ByteArrayInputStream])))

(defn- transit-read [string]
  #?(:clj  (-> ^String string
               .getBytes
               ByteArrayInputStream.
               (transit/reader :json)
               transit/read)
     :cljs (transit/read (transit/reader :json) string)))

(defn- transit-write [value]
  #?(:clj (let [out (ByteArrayOutputStream. 1024)]
            (transit/write
             (transit/writer out :json {:transform transit/write-meta})
             value)
            (.toString out))
     :cljs (transit/write
            (transit/writer :json {:transform transit/write-meta})
            value)))

(defn pass [v]
  (cson/read (cson/write v)))

(deftest simple-values
  (are [value]
       (= value (pass value))
    nil
    0
    1.0
    #?(:clj 42N :cljs (js/BigInt "42"))
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

(deftest seq-collections
  (are [value]
       (= (seq value) (pass (seq value)))
    '(0)
    [0]
    #{0}
    {0 0}))

#?(:clj  (defn random-uuid [] (java.util.UUID/randomUUID))
   :clje (defn random-uuid [] (erlang.util.UUID/random)))

(deftest tagged-objects
  (let [inst #?(:clj  (java.util.Date.)
                :cljs (js/Date.)
                :clje (erlang.util.Date.))]
    (is (= inst (pass inst))))
  (let [inst (random-uuid)]
    (is (= inst (pass inst)))))

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
   (defmacro simple-benchmark
     [bindings expr iterations & {:keys [print-fn] :or {print-fn 'println}}]
     (let [bs-str   (pr-str bindings)
           expr-str (pr-str expr)]
       `(let ~bindings
          (let [start#   (System/currentTimeMillis)
                ret#     (dotimes [_# ~iterations] ~expr)
                end#     (System/currentTimeMillis)
                elapsed# (- end# start#)]
            (~print-fn (str ~bs-str ", " ~expr-str ", "
                            ~iterations " runs, " elapsed# " msecs")))))))

(defn json-parse [string]
  #?(:clj  (dorun (json/parse-string string))
     :cljs (.parse js/JSON string)
     :clje (jiffy/decode string)))

(defn json-stringify [value]
  #?(:clj  (json/generate-string value)
     :cljs (.stringify js/JSON value)
     :clje (jiffy/encode value)))

(def input (into [] (range 100000)))

(comment
  (deftest write-benchmark
    (simple-benchmark
     [v #?(:clj  input
           :cljs (clj->js input))]
     (json-stringify v)
     100)
    (simple-benchmark
     [v input]
     (transit-write v)
     100)
    (simple-benchmark
     [v input]
     (cson/write v)
     100))

  (deftest read-benchmark
    (simple-benchmark
     [v (json-stringify #?(:clj  input
                           :cljs (clj->js input)))]
     (json-parse v)
     100)
    (simple-benchmark
     [v (transit-write composite-value)]
     (transit-read v)
     1000)
    (simple-benchmark
     [v (cson/write composite-value)]
     (cson/read v)
     1000)))

(deftest rich-benchmark
  (simple-benchmark
   [v composite-value]
   (transit-write v)
   1000)
  (simple-benchmark
   [v composite-value]
   (cson/write v)
   1000))
