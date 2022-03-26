(ns ^:no-doc portal.resources
  #?(:cljs (:require-macros portal.resources))
  #?(:clj (:require [clojure.java.io :as io])))

#?(:clj
   (defmacro inline [resource-name]
     (slurp (io/resource resource-name))))
