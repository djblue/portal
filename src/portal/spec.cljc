(ns ^:no-doc portal.spec
  (:require [clojure.spec.alpha :as s]
            [portal.colors :as c]))

(s/def ::options (s/keys :opt [::c/theme]))

(defn assert-options [options]
  (let [options (select-keys options [::c/theme])
        parsed  (s/conform ::options options)]
    (when (= parsed ::s/invalid)
      (throw (ex-info "Invalid options" (s/explain-data ::options options))))))

