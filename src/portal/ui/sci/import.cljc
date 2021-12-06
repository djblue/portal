(ns portal.ui.sci.import
  (:refer-clojure :exclude [import])
  #?(:cljs (:require-macros portal.ui.sci.import)))

(defn- sci-import [symbols]
  (let [ns (gensym)]
    (reduce-kv
     (fn [namespaces namespace var-symbols]
       (let [namespace (if (= namespace 'cljs.core) 'clojure.core namespace)]
         (assoc
          namespaces
          (list 'quote namespace)
          `(let [~ns (sci.core/create-ns '~namespace nil)]
             ~(reduce
               (fn [mapped var-symbol]
                 (assoc mapped
                        (list 'quote (symbol (name var-symbol)))
                        `(sci.core/copy-var ~var-symbol ~ns)))
               {:obj ns}
               var-symbols)))))
     {}
     (group-by (comp symbol namespace) symbols))))

(defmacro import [& symbols] (sci-import symbols))
