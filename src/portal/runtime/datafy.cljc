(ns ^:no-doc portal.runtime.datafy)

(defn datafy
  "Attempts to return x as data.
  datafy will return the value of clojure.core.protocols/datafy. If
  the value has been transformed and the result supports
  metadata, :clojure.datafy/obj will be set on the metadata to the
  original value of x, and :clojure.datafy/class to the name of the
  class of x, as a symbol."
  [x]
  x)

(defn nav [_coll _k v] v)