(ns portal.ui.viewer.edn
  (:require [clojure.edn :as edn]
            [portal.ui.inspector :refer [inspector]]))

(defn- parse-edn [edn-string]
  (try (edn/read-string edn-string)
       (catch :default _e ::invalid)))

(defn edn? [value] (string? value))

(defn inspect-edn [settings edn-string]
  [inspector settings (parse-edn edn-string)])
