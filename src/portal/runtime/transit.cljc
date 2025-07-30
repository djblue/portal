(ns ^:no-doc portal.runtime.transit
  (:refer-clojure :exclude [read])
  #?(:org.babashka/nbb (:require)
     :joyride          (:require [cljs.core])
     :clj              (:require [cognitect.transit :as transit])
     :cljs             (:require [cognitect.transit :as transit]))
  #?(:clj (:import [java.io ByteArrayOutputStream ByteArrayInputStream])))

(defn read [string]
  #?(:org.babashka/nbb
     (throw (ex-info "transit/read not supported in nbb" {:string string}))
     :joyride
     (throw (ex-info "transit/read not supported in joyride" {:string string}))
     :lpy
     (throw (ex-info "transit/read not supported in basilisp" {:string string}))

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
     :lpy
     (throw (ex-info "transit/read not supported in basilisp" {:value value}))

     :clj (let [out (ByteArrayOutputStream. 1024)]
            (transit/write
             (transit/writer out :json {:transform transit/write-meta})
             value)
            (.toString out))
     :cljs (transit/write
            (transit/writer :json {:transform transit/write-meta})
            value)))
