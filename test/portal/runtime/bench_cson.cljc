(ns portal.runtime.bench-cson
  (:require [clojure.edn :as edn]
            [portal.bench :as b]
            [portal.runtime.cson :as cson]
            [portal.runtime.transit :as transit]))

(defn run [v n]
  (with-meta
    #?(:cljr
       [(b/run :cson (-> v cson/write cson/read) n)
        (b/run :edn (-> v pr-str edn/read-string) n)]
       :default
       [(b/run :transit (-> v transit/write transit/read) n)
        (b/run :cson (-> v cson/write cson/read) n)
        (b/run :edn (-> v pr-str edn/read-string) n)])
    {:portal.viewer/default :portal.viewer/table
     :portal.viewer/table {:columns [:label :min :max :avg :total]}}))
