(ns ^:no-doc portal.runtime.json
  (:refer-clojure :exclude [read])
  #?(:bb   (:require [cheshire.core :as json])
     :clj  (:require [clojure.data.json :as json])
     :cljr (:require [clojure.data.json :as json]
                     [portal.runtime.clr.assembly])))

(defn write [value]
  #?(:bb   (json/generate-string value)
     :clj  (json/write-str value)
     :cljr (json/write-str value)
     :cljs (.stringify js/JSON value)))

(defn read
  ([string]
   (read string {:key-fn keyword}))
  ([string opts]
   #?(:bb   (json/parse-string string (:key-fn opts))
      :clj  (json/read-str string :key-fn (:key-fn opts))
      :cljr (json/read-str string :key-fn (:key-fn opts))
      :cljs (js->clj (.parse js/JSON string)
                     :keywordize-keys
                     (= keyword (:key-fn opts))))))

(defn read-stream [stream]
  #?(:bb   (json/parse-stream stream keyword)
     :clj  (json/read stream :key-fn keyword)
     :cljs (throw (ex-info "Unsupported in cljs" {:stream stream}))))
