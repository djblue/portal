(ns portal.rpc
  (:require [cognitect.transit :as t]
            [com.cognitect.transit.types :as ty]))

;; Since any object can have metadata and all unknown objects in portal
;; are encoded as tagged values, if any of those objects have metadata, it
;; will be problematic. Tagged values are represented in transit with
;; ty/TaggedValue which doesn't support metadata, leading to error when
;; reading. Implementing the metadata protocols will fix the issue for
;; now. The crux of the problem is that write-meta gets to the metadata
;; before it can be hidden in the representation.
(extend-type ty/TaggedValue
  IMeta
  (-meta [this]
    (:meta (.-rep this)))

  IWithMeta
  (-with-meta [this m]
    (t/tagged-value
     (.-tag this)
     (assoc (.-rep this) :meta m))))

(defn json->edn [json]
  (let [r (t/reader :json)] (t/read r json)))

(defn edn->json [edn]
  (let [w (t/writer :json {:transform t/write-meta})]
    (t/write w edn)))

(defn send! [msg]
  (-> (js/fetch
       "/rpc"
       #js {:method "POST" :body (edn->json msg)})
      (.then #(.text %))
      (.then json->edn)))

