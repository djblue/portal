(ns ^:no-doc portal.runtime.macros
  #?(:cljs (:require-macros portal.runtime.macros)))

#?(:clj
   (defn- resolve-var [& args]
     (apply (requiring-resolve 'cljs.analyzer/resolve-var) args)))

#?(:clj
   (defn- ->type [type]
     (if (and (= type 'js/BigInt)
              (contains? @(requiring-resolve 'cljs.core/base-type) 'bigint))
       'bigint
       type)))

#?(:clj
   (defmacro extend-type? [type & args]
     (when (or (= (namespace type) "js")
               (:type (resolve-var &env type)))
       `(extend-type ~(->type type) ~@args)))
   :joyride
   (defmacro extend-type? [type & args]
     (when (contains? #{'js/BigInt 'js/URL} type)
       `(extend-type ~type ~@args)))
   :cljs
   (defmacro extend-type? [type & args]
     `(when (exists? ~type)
        (extend-type ~type ~@args))))
