(ns ^:no-doc portal.async
  (:refer-clojure :exclude [let try promise])
  #?(:portal (:import) :cljs (:require-macros portal.async)))

(defmacro do [& body]
  (reduce
   (fn [chain form]
     `(.then ~chain
             (fn [] (.resolve js/Promise ~form))))
   `(.resolve js/Promise nil)
   body))

(defmacro let [bindings & body]
  (->> (partition-all 2 bindings)
       reverse
       (reduce (fn [body [n v]]
                 `(.then (.resolve js/Promise ~v)
                         (fn [~n] ~body)))
               `(portal.async/do ~@body))))

(defmacro try [& body]
  (reduce
   (fn [chain form]
     (cond
       (and (coll? form) (= 'finally (first form)))
       `(.finally ~chain
                  (fn []
                    (portal.async/do ~@(rest form))))

       (and (coll? form) (= 'catch (first form)))
       (clojure.core/let [[_ _type ex-binding & body] form]
         `(.catch ~chain
                  (fn [~ex-binding]
                    (portal.async/do ~@body))))

       :else
       `(.then ~chain
               (fn []
                 (.resolve js/Promise ~form)))))
   `(.resolve js/Promise nil)
   body))

#?(:cljs (defn race [& args] (.race js/Promise args)))