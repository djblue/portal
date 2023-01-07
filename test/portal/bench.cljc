(ns portal.bench
  #?(:cljs (:refer-clojure :exclude [simple-benchmark]))
  #?(:cljs (:require-macros portal.bench)))

(defn- now
  ([]
   #?(:clj  (System/nanoTime)
      :cljr (.Ticks (System.DateTime/Now))
      :cljs (if (exists? js/process)
              (.hrtime js/process)
              (.now js/Date))))
  ([a]
   #?(:clj  (/ (- (now) a) 1000000.0)
      :cljr (/ (- (now) a) 10000.0)
      :cljs (if (exists? js/process)
              (let [[a b] (.hrtime js/process a)]
                (+ (* a 1000.0) (/ b  1000000.0)))
              (- (.now js/Date) a)))))
(defn floor [v]
  #?(:cljr (Math/Floor v) :default (Math/floor v)))

(defn trunc [v]
  (/ (floor (* 100 v)) 100.0))

(defn- simple-stats [expr results]
  (let [n       (count results)
        results (into [] (sort results))
        median  (nth results (quot n 2))
        total   (reduce + results)]
    {:label expr
     :unit  :ms
     :iter  n
     :min   (first results)
     :max   (last results)
     :avg   (trunc (/ total n))
     :med   median
     :total (trunc total)}))

(defn run* [expr f n]
  (dotimes [_ n] (f))
  (simple-stats
   expr
   (loop [i 0 results (transient [])]
     (if (== i n)
       (persistent! results)
       (let [start (now)
             _     (f)
             end   (now start)]
         (recur (unchecked-inc i)
                (conj! results (trunc end))))))))

(defmacro run [label expr n] `(run* ~label #(do ~expr) ~n))
