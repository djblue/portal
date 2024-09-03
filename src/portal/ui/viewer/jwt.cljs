(ns ^:no-doc portal.ui.viewer.jwt
  (:require [clojure.string :as str]
            [goog.crypt.base64 :as Base64]
            [portal.ui.inspector :as ins]
            [portal.ui.parsers :as p]))

(defn- parse-json [value]
  (js->clj (.parse js/JSON (js/atob value)) :keywordize-keys true))

(defn parse-jwt [jwt]
  (try
    (let [[header payload signature] (str/split jwt ".")]
      (with-meta
        {:jwt/header (parse-json header)
         :jwt/payload
         (with-meta (parse-json payload)
           {:portal.viewer/for
            {:auth_time :portal.viewer/date-time
             :exp :portal.viewer/date-time
             :iat :portal.viewer/date-time
             :nbf :portal.viewer/date-time}})
         :jwt/signature
         (Base64/decodeStringToUint8Array signature)}
        {:portal.viewer/for
         {:jwt/signature :portal.viewer/bin}}))
    (catch :default e (ins/error->data e))))

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
