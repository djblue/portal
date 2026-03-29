(ns ^:no-doc portal.ui.viewer.jwt
  (:require [clojure.string :as str]
            #?(:cljs [goog.crypt.base64 :as Base64])
            [portal.runtime.json :as json]
            [portal.ui.inspector :as ins]
            [portal.ui.parsers :as p])
  #?(:clj (:import [java.util Base64])))

(defn- decode ^bytes [^String s]
  #?(:clj  (.decode (Base64/getUrlDecoder) s)
     :cljs (Base64/decodeStringToUint8Array s)))

(defn- atob [s] #?(:clj (String. (decode s)) :cljs (js/atob s)))

(defn parse-jwt [jwt]
  (try
    (let [[header payload signature] (str/split jwt #"\.")]
      (with-meta
        {:jwt/header (json/read (atob header))
         :jwt/payload
         (with-meta (json/read (atob payload))
           {:portal.viewer/for
            {:auth_time :portal.viewer/date-time
             :exp :portal.viewer/date-time
             :iat :portal.viewer/date-time
             :nbf :portal.viewer/date-time}})
         :jwt/signature (decode signature)}
        {:portal.viewer/for
         {:jwt/signature :portal.viewer/bin}}))
    (catch #?(:clj Exception :cljs :default) e
      #?(:clj  (Throwable->map e)
         :cljs (ins/error->data e)))))

(defmethod p/parse-string :format/jwt [_ s] (parse-jwt s))

(defn inspect-jwt [jwt]
  [ins/tabs
   {:portal.viewer/jwt (parse-jwt jwt)
    "..."              jwt}])

(def viewer
  {:predicate string?
   :component #'inspect-jwt
   :name :portal.viewer/jwt
   :doc "Parse a string as a JWT. Will render error if parsing fails."})
