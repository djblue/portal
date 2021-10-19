(ns portal.runtime.json
  (:refer-clojure :exclude [read]))

(defn write [value]
  (#?(:bb   cheshire.core/generate-string
      :clj  (requiring-resolve 'clojure.data.json/write-str)
      :cljs #(.stringify js/JSON %))
   value))

(defn read [string]
  (#?(:bb   cheshire.core/parse-string
      :clj  (requiring-resolve 'clojure.data.json/read-str)
      :cljs #(.parse js/JSON %))
   string))
