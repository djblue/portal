(ns ^:no-doc portal.nrepl
  (:require [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.transport :as transport]
            [portal.api :as p]
            [portal.runtime :as rt])
  (:import [nrepl.transport Transport]))

; fork of https://github.com/DaveWM/nrepl-rebl/blob/master/src/nrepl_rebl/core.clj

(defn form-from-cursive? [form]
  (and (sequential? form)
       (symbol? (first form))
       (= "cursive.repl.runtime" (namespace (first form)))))

(defn read-string* [s]
  (when s
    (try
      (read-string s)
      (catch Exception _e nil))))

(defrecord PortalTransport [transport handler-msg]
  Transport
  (recv [_this timeout]
    (transport/recv transport timeout))
  (send [_this {:keys [value] :as msg}]
    (transport/send transport msg)
    (when-let [code-form (read-string* (:code handler-msg))]
      (when (and (some? value)
                 (not (form-from-cursive? code-form)))
        (rt/update-value
         {:code-form code-form :value value})))
    transport))

(defn wrap-portal [handler]
  (p/open)
  (fn [msg]
    (-> msg
        (update :transport ->PortalTransport msg)
        handler)))

(set-descriptor! #'wrap-portal
                 {:requires #{}
                  :expects #{"eval"}
                  :handles {}})


