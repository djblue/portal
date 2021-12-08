(ns ^:no-doc portal.runtime.macros
  #?(:cljs (:require-macros portal.runtime.macros)))

#?(:clj
   (defn- resolve-var [& args]
     (apply (requiring-resolve 'cljs.analyzer/resolve-var) args)))

#?(:clj
   (defmacro extend-type? [type & args]
     (when (or (= (namespace type) "js")
               (:type (resolve-var &env type)))
       `(extend-type ~type ~@args)))
   :cljs
   (defmacro extend-type? [type & args]
     `(when (exists? ~type)
        (extend-type ~type ~@args))))
