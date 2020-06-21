(ns portal.nrepl
  (:require [portal.main :as m]
            [nrepl.middleware :refer [set-descriptor!]]
            [nrepl.transport :as transport]
            [nrepl.middleware.session :refer [session]])
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
      (catch Exception e nil))))

(defrecord PortalTransport [transport handler-msg]
  Transport
  (recv [this timeout]
    (transport/recv transport timeout))
  (send [this {:keys [value] :as msg}]
    (transport/send transport msg)
    (when-let [code-form (read-string* (:code handler-msg))]
      (when (and (some? value)
                 (not (form-from-cursive? code-form)))
        (m/update-value
         {:code-form code-form :value value})))
    transport))

(defn wrap-portal [handler]
  (m/inspect 1)
  (fn [msg]
    (-> msg
        (update :transport ->PortalTransport msg)
        handler)))

(set-descriptor! #'wrap-portal
                 {:requires #{}
                  :expects #{"eval"}
                  :handles {}})


