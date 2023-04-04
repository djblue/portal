(ns ^:no-doc portal.async
  (:refer-clojure :exclude [let promise])
  #?(:portal (:import) :cljs (:require-macros portal.async)))

(defmacro do [& body]
  (reduce
   (fn [chain form]
     `(.then ~chain
             (fn [] (js/Promise.resolve ~form))))
   `(js/Promise.resolve nil)
   body))

(defmacro let [bindings & body]
  (->> (partition-all 2 bindings)
       reverse
       (reduce (fn [body [n v]]
                 `(.then (js/Promise.resolve ~v)
                         (fn [~n] ~body)))
               `(portal.async/do ~@body))))

