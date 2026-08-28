(ns portal.relay.client.web
  (:require
   [portal.client.common :as common]))

(defn submit
  ([value] (submit nil value))
  ([{:keys [encoding]
     :or   {encoding :edn}} value])
  (js/window.postMessage
   #js {:portal-relay? true
        :body          (common/serialize encoding value)
        :content-type  (encoding->content-type encoding)}
   "*"))
