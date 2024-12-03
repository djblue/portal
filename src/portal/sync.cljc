(ns portal.sync
  {:no-doc true}
  (:refer-clojure :exclude [let try]))

(defmacro let [bindings & body]
  `(clojure.core/let ~bindings ~@body))

(defmacro try [& body] `(~'try ~@body))
