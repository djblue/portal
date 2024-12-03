(ns portal.runtime.json
  {:no-doc true}
  (:refer-clojure :exclude [read])
  (:require
   #?@(:bb
       [[cheshire.core :as json]]

       :clj
       [[clojure.data.json :as json]]

       :cljr
       [[clojure.data.json :as json]
        [portal.runtime.clr.assembly]])))

(defn write [value]
  #?(:bb   (json/generate-string value)
     :clj  (json/write-str value)
     :cljr (json/write-str value)
     :cljs (.stringify js/JSON (clj->js value))))

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
