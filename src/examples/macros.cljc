(ns examples.macros
  #?(:cljs (:require-macros examples.macros)))

#?(:clj  (defmacro read-file [file-name] (slurp file-name))
   :clje (defn read-file [file-name] file-name))
