(ns portal.runtime.json-buffer-test
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime.json-buffer :as b]))

(defn- write-json [buffer _]
  (-> buffer
      (b/push-null)
      (b/push-long 0)
      (b/push-double 0.5)
      (b/push-bool true)
      (b/push-bool false)
      (b/push-string "hello")))

(deftest json-buffer
  (let [r (b/->reader (b/with-buffer write-json nil))]
    (is (nil? (b/next-null r)))
    (is (zero? (b/next-long r)))
    (is (= 0.5 (b/next-double r)))
    (is (true? (b/next-bool r)))
    (is (false? (b/next-bool r)))
    (is (= "hello" (b/next-string r))))
  (let [r (b/->reader (b/with-buffer write-json nil))]
    (is (nil? (b/next-value r)))
    (is (zero? (b/next-value r)))
    (is (= 0.5 (b/next-value r)))
    (is (true? (b/next-value r)))
    (is (false? (b/next-value r)))
    (is (= "hello" (b/next-value r)))))
