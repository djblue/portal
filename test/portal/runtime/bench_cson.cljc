(ns portal.runtime.bench-cson
  (:require [portal.bench :as b]
            [portal.runtime.cson :as cson]
            [portal.runtime.transit :as transit]))

(defn run [v n]
  (with-meta
    [(b/run :transit (-> v transit/write transit/read) n)
     (b/run :cson (-> v cson/write cson/read) n)]
    {:portal.viewer/default :portal.viewer/table
     :portal.viewer/table {:columns [:label :min :max :avg :total]}}))
