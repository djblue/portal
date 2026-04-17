(ns ^:no-doc portal.sync
  (:refer-clojure :exclude [let]))

(defmacro do [& body] `(~'do ~@body))

(defmacro let [bindings & body]
  `(~'let ~bindings ~@body))

(defmacro try [& body] `(~'try ~@body))
