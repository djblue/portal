(ns ^:no-doc portal.ui.viewer.transit
  (:require [portal.runtime.transit :as transit]
            [portal.ui.inspector :as ins]
            [portal.ui.parsers :as p]))

(defn- parse-transit [transit-string]
  (try (transit/read transit-string)
       (catch #?(:clj Exception :cljs :default) e
         #?(:clj  (Throwable->map e)
            :cljs (ins/error->data e)))))

(defmethod p/parse-string :format/transit [_ s] (parse-transit s))

(defn transit? [value] (string? value))

(defn inspect-transit [transit-string]
  [ins/tabs
   {:portal.viewer/transit (parse-transit transit-string)
    "..."                  transit-string}])

(def viewer
  {:predicate transit?
   :component #'inspect-transit
   :name :portal.viewer/transit
   :doc "Parse a string as transit. Will render error if parsing fails."})