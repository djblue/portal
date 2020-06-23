(ns portal.sync
  (:refer-clojure :exclude [let]))

(defmacro let [bindings & body]
  `(clojure.core/let ~bindings ~@body))
