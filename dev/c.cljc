(ns c
  (:require [portal.console :as console])
  #?(:cljs (:require-macros portal.console)))

(defmacro log [expr] (console/capture :info &form expr))
