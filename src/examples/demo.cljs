(ns examples.demo
  (:require [portal.core :as portal]
            [examples.data :refer [data]]
            [portal.async :as a]
            [clojure.datafy :refer [datafy nav]]))

(defn send! [msg]
  (js/Promise.resolve
   (case (:op msg)
     :portal.rpc/clear-values nil
     :portal.rpc/load-state
     {:portal/complete? true}
     :portal.rpc/on-nav
     (a/let [res (apply nav (:args msg))]
       {:value (datafy res)}))))

(defn main! []
  (portal/main!
   (merge
    (portal/get-actions send!)
    {:portal/value data})))

(defn reload! [])
