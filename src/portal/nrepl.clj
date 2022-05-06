(ns ^:no-doc portal.nrepl
  (:require [clojure.datafy :as d]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.middleware.caught :as caught]
            [nrepl.middleware.print :as print]
            [nrepl.transport :as transport]
            [portal.api :as p])
  (:import [java.util Date]
           [nrepl.transport Transport]))

; fork of https://github.com/DaveWM/nrepl-rebl/blob/master/src/nrepl_rebl/core.clj

(defn- get-result [response]
  (cond
    (contains? response :nrepl.middleware.caught/throwable)
    {:level  :error
     :file   "*repl*"
     :line   1
     :column 1
     :result (d/datafy (:nrepl.middleware.caught/throwable response))}

    (contains? response :value)
    {:level  :info
     :file   "*repl*"
     :line   1
     :column 1
     :result (:value response)}))

(defrecord PortalTransport [transport handler-msg]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this  msg]
    (transport/send transport msg)
    (when-let [result (get-result msg)]
      (-> result
          (merge
           (select-keys handler-msg [:ns :file :column :line :code]))
          (update :ns (fnil symbol 'user))
          (assoc :time     (Date.)
                 :runtime :clj)
          p/submit))
    transport))

(defn wrap-portal [handler]
  (fn [msg]
    (handler
     (cond-> msg
       (= (:op msg) "eval")
       (update :transport ->PortalTransport msg)))))

(set-descriptor! #'wrap-portal
                 {:requires #{#'print/wrap-print
                              #'caught/wrap-caught}
                  :expects #{"eval"}
                  :handles {}})
