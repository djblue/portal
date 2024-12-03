(ns portal.runtime.transit
  {:no-doc true}
  (:refer-clojure :exclude [read])
  (:require
   #?(:clj [cognitect.transit :as transit]
      :cljs [cognitect.transit :as transit]
      :joyride [cljs.core]))
  #?(:clj
     (:import
      (java.io ByteArrayInputStream ByteArrayOutputStream))))

(defn read [string]
  #?(:org.babashka/nbb
     (throw (ex-info "transit/read not supported in nbb" {:string string}))
     :joyride
     (throw (ex-info "transit/read not supported in joyride" {:string string}))

     :clj  (-> ^String string
               .getBytes
               ByteArrayInputStream.
               (transit/reader :json)
               transit/read)
     :cljs (transit/read (transit/reader :json) string)))

(defn write [value]
  #?(:org.babashka/nbb
     (throw (ex-info "transit/write not supported in nbb" {:value value}))
     :joyride
     (throw (ex-info "transit/write not supported in joyride" {:value value}))

     :clj (let [out (ByteArrayOutputStream. 1024)]
            (transit/write
              (transit/writer out :json {:transform transit/write-meta})
              value)
            (.toString out))
     :cljs (transit/write
             (transit/writer :json {:transform transit/write-meta})
             value)))
