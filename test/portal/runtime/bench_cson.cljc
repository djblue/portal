(ns portal.runtime.bench-cson
  (:require [portal.bench :as b]
            [portal.runtime.cson :as cson]
            [portal.runtime.transit :as transit]))

(defn run [v n]
  (assert (= v (-> v transit/write  transit/read)))
  (assert (= v (-> v cson/write     cson/read)))

  (b/simple-benchmark [] (-> v transit/write transit/read) n)
  (b/simple-benchmark [] (-> v cson/write cson/read) n))
