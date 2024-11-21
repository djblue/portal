(ns ^:no-doc examples.macros
  #?(:portal (:import) :cljs (:require-macros examples.macros)))

#?(:clj  (defmacro read-file [file-name] (slurp file-name))
   :cljs (defn read-file [_file-name] ::missing)
   :cljr (defmacro read-file [file-name] (slurp file-name :enc "utf8"))
   :lpy  (defn read-file [file-name] (slurp file-name)))
