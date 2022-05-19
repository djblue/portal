(ns portal.ui.viewer.edn
  (:require [clojure.edn :as edn]
            [portal.ui.inspector :as ins]))

(defn read-string [edn-string]
  (try (edn/read-string {:default tagged-literal} edn-string)
       (catch :default e (ins/error->data e))))

(defn edn? [value] (string? value))

(defn inspect-edn [edn-string]
  [ins/tabs
   {:portal.viewer/edn (read-string edn-string)
    "..."              edn-string}])

(def viewer
  {:predicate edn?
   :component inspect-edn
   :name :portal.viewer/edn})
