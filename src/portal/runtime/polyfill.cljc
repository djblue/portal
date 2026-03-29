(ns ^:no-doc portal.runtime.polyfill
  (:refer-clojure :exclude [parse-uuid random-uuid update-keys]))

(defn random-uuid []
  #?(:clj  (java.util.UUID/randomUUID)
     :cljs (cljs.core/random-uuid)
     :cljr (System.Guid)
     :lpy  (uuid/UUID)))

(defn parse-uuid [^String s]
  #?(:clj  (java.util.UUID/fromString s)
     :cljs (uuid s)
     :cljr (System.Guid/Parse s)
     :lpy  (uuid/UUID (json/next-string buffer))))

(defn update-keys [m f]
  (with-meta
    (persistent!
     (reduce-kv
      (fn [acc k v] (assoc! acc (f k) v))
      (transient {})
      m))
    (meta m)))