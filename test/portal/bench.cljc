(ns portal.bench
  #?(:cljs (:refer-clojure :exclude [simple-benchmark]))
  #?(:cljs (:require-macros portal.bench))
  #?(:lpy (:import [math :as Math]
                   [time :as time])))

(defn- now
  ([]
   #?(:clj  (System/nanoTime)
      :cljr (.Ticks (System.DateTime/Now))
      :cljs (if (exists? js/process)
              (.hrtime js/process)
              (.now js/Date))
      :lpy  (time/process_time_ns)))
  ([a]
   #?(:clj  (/ (- (now) a) 1000000.0)
      :cljr (/ (- (now) a) 10000.0)
      :cljs (if (exists? js/process)
              (let [[a b] (.hrtime js/process a)]
                (+ (* a 1000.0) (/ b  1000000.0)))
              (- (.now js/Date) a))
      :lpy (/ (- (now) a) 1000000.0))))

(defn floor [v]
  #?(:cljr (Math/Floor v) :default (Math/floor v)))

(defn trunc [v]
  (/ (floor (* 100 v)) 100.0))

(defn- simple-stats [results]
  (let [n       (count results)
        results (into [] (sort results))
        median  (nth results (quot n 2))
        total   (reduce + results)]
    ^{:portal.viewer/for
      {:min :portal.viewer/duration-ms
       :max :portal.viewer/duration-ms
       :avg :portal.viewer/duration-ms
       :med :portal.viewer/duration-ms
       :total :portal.viewer/duration-ms}}
    {:iter  n
     :min   (first results)
     :max   (last results)
     :avg   (trunc (/ total n))
     :med   median
     :total (trunc total)}))

(defn run* [f ^long n]
  (dotimes [_ n] (f))
  (simple-stats
   (loop [i 0 results (transient [])]
     (if (== i n)
       (persistent! results)
       (let [start (now)
             _     (f)
             end   (now start)]
         (recur (unchecked-inc i)
                (conj! results (trunc end))))))))

(defmacro run [expr n] `(run* #(do ~expr) ~n))
