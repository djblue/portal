(ns portal.runtime.macros
  #?(:cljs (:require-macros portal.runtime.macros)))

#?(:clj
   (defn- resolve-var [& args]
     (apply (requiring-resolve 'cljs.analyzer/resolve-var) args)))

#?(:clj
   (defmacro extend-type? [type & args]
     (when (:type (resolve-var &env type))
       `(extend-type ~type ~@args))))
