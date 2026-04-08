(ns ^:no-doc portal.ui.lazy
  #?(:clj (:refer-clojure :exclude [lazy-seq random-uuid]))
  #?(:clj (:require
           [portal.runtime.polyfill :refer [random-uuid]]
           [portal.ui.react :as react])))

(def count-limit 100000)

(defn safe-count
  ([coll]
   (safe-count coll count-limit))
  ([coll count-limit]
   (if (counted? coll)
     [(count coll) false]
     (let [n (count (take (inc count-limit) coll))
           truncated? (> n count-limit)]
       [(if truncated? count-limit n) truncated?]))))

(defn safe-count-str [coll]
  (let [[n truncated?] (safe-count coll)]
    (if truncated? (str count-limit "+") (str n))))

(defn- lazy-seq? [value]
  #?(:clj (or (instance? clojure.lang.LazySeq value)
              (instance? clojure.lang.Iterate value)
              (instance? clojure.lang.Range value))
     :cljs (or (instance? cljs.core/LazySeq value)
               (instance? cljs.core/Iterate value)
               (instance? cljs.core/Range value))))

(defn safe-seq [value]
  (cond->> value (lazy-seq? value) (take count-limit)))

#?(:clj
   (defn lazy-seq
     ([coll]
      (lazy-seq coll nil))
     ([coll {:keys [default-take step]
             :or   {default-take 0 step 10}}]
      (let [[n set-n!] (react/use-state default-take)
            [head tail] (split-at n coll)]
        [:<>
         head
         (when (seq tail)
           [:visible-sensor
            {:id (random-uuid)
             :on-visible (fn [_] (set-n! (+ n step)))}])]))))

#?(:clj
   (defn use-visible []
     (let [[visible? set-visible!] (react/use-state false)]
       [(when-not visible?
          [:visible-sensor
           {:id (random-uuid)
            :on-visible (fn [_] (set-visible! true))}])
        visible?])))

#?(:clj
   (defn lazy-render [child]
     (let [[show set-show!] (react/use-state false)]
       (if show
         child
         [:<>
          [:visible-sensor
           {:on-visible (fn [_] (set-show! true))}]
          [:div {:style {:height "50vh"}}]]))))