(ns portal.viewer.http
  (:require [clojure.spec.alpha :as spec]
            [portal.inspector :as ins :refer [inspector]]
            [portal.rpc :as rpc]
            [portal.styled :as s]
            [reagent.core :as r]))

(spec/def ::http-request
  (spec/keys
   :req-un [::url]
   :opt-un [::headers ::method ::body]))

(spec/def ::url string?)
(spec/def ::method #{"GET" "POST" "PUT" "PATCH" "DELETE"})
(spec/def ::headers map?)
(spec/def ::body string?)

(defn http-request? [value]
  (spec/valid? ::http-request value))

(defn http-request [request]
  (rpc/send! {:op :portal.rpc/http-request :request request}))

(defn inspect-http []
  (let [response (r/atom nil)]
    (fn [settings value]
      [s/div
       [s/button
        {:on-click (fn []
                     (->  (http-request value)
                          (.then #(reset! response (:response %)))))}
        "send request"]
       [inspector settings value]
       (when @response
         [inspector settings @response])])))
