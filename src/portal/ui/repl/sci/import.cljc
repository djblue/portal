(ns ^:no-doc portal.ui.repl.sci.import
  (:refer-clojure :exclude [import])
  #?(:cljs (:require-macros portal.ui.repl.sci.import))
  #?(:cljs (:require [sci.core :as sci])))

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

#?(:cljs
   (defn import-ns* [namespace publics]
     {namespace
      (let [ns (sci/create-ns namespace nil)]
        (reduce
         (fn [ns-map [var-name var]]
           (let [m (meta var)]
             (if (:private m)
               ns-map
               (assoc ns-map var-name
                      (sci/new-var (symbol var-name) @var (assoc m :ns ns))))))
         {:obj ns}
         publics))}))

(defmacro import-ns [& namespaces]
  `(merge
    ~@(for [ns namespaces]
        `(import-ns* '~ns (ns-publics '~ns)))))
