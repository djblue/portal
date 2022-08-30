(ns ^:no-doc portal.runtime.json
  (:refer-clojure :exclude [read])
  (:require
   #?(:bb  [cheshire.core :as json]
      :clj [clojure.data.json :as json])))

(defn write [value]
  #?(:bb   (json/generate-string value)
     :clj  (json/write-str value)
     :cljs (.stringify js/JSON value)))

(defn read [string]
  #?(:bb   (json/parse-string string keyword)
     :clj  (json/read-str string :key-fn keyword)
     :cljs (.parse js/JSON string)))

(defn read-stream [stream]
  #?(:bb   (json/parse-stream stream keyword)
     :clj  (json/read stream :key-fn keyword)
     :cljs (throw (ex-info "Unsupported in cljs" {:stream stream}))))
