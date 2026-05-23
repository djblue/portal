(ns ^:no-doc portal.runtime.cson.base64
  #?(:clj (:import [java.util Base64])
     :joyride (:require clojure.core)
     :org.babashka/nbb (:require)
     :cljs (:require [goog.crypt.base64 :as Base64])
     :lpy (:import [base64 :as base64])))

(defn encode [byte-array]
  #?(:clj (.encodeToString (Base64/getEncoder) byte-array)
     :joyride (.toString (.from js/Buffer byte-array) "base64")
     :org.babashka/nbb (.toString (.from js/Buffer byte-array) "base64")
     :cljs (Base64/encodeByteArray byte-array)
     :cljr (Convert/ToBase64String byte-array)
     :lpy (.decode (base64/b64encode byte-array) "ascii")))

(defn decode [string]
  #?(:clj (.decode (Base64/getDecoder) ^String string)
     :joyride (js/Uint8Array. (.from js/Buffer string "base64"))
     :org.babashka/nbb (js/Uint8Array. (.from js/Buffer string "base64"))
     :cljs (Base64/decodeStringToUint8Array string)
     :cljr (Convert/FromBase64String string)
     :lpy (base64/b64decode string)))