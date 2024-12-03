(ns examples.macros
  {:no-doc true}
  #?(:cljs
     (:require-macros
      [examples.macros])))

#?(:clj  (defmacro read-file [file-name] (slurp file-name))
   :cljs (defn read-file [_file-name] ::missing)
   :cljr (defmacro read-file [file-name] (slurp file-name :enc "utf8")))
