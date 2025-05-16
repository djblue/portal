(ns ^:no-doc portal.sync
  (:refer-clojure :exclude [let try]))

(defmacro let [bindings & body]
  `(~'let ~bindings ~@body))

(defmacro try [& body] `(~'try ~@body))
