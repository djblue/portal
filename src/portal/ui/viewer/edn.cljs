(ns portal.ui.viewer.edn
  (:require [clojure.edn :as edn]
            [portal.ui.inspector :as ins]))

(defn- parse-edn [edn-string]
  (try (edn/read-string edn-string)
       (catch :default _e ::invalid)))

(defn edn? [value] (string? value))

(defn inspect-edn [edn-string]
  [ins/tabs
   {:portal.viewer/edn (parse-edn edn-string)
    "..."              edn-string}])

(def viewer
  {:predicate edn?
   :component inspect-edn
   :name :portal.viewer/edn})
