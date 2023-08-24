(ns portal.runtime.bench-cson
  (:require [clojure.edn :as edn]
            [examples.data :as d]
            [portal.bench :as b]
            [portal.runtime.cson :as cson]
            [portal.runtime.transit :as transit]))

(defn- pr-meta [v] (binding [*print-meta* true] (pr-str v)))

(def bench-data
  {:platform-data (select-keys d/platform-data [::d/uuid ::d/date])
   :basic-data (dissoc d/basic-data ::d/range)
   :hiccup-data d/hiccup
   :data-visualization d/data-visualization
   :string-data d/string-data
   :log-data d/log-data
   :profile-data d/log-data
   :prepl-data d/prepl-data
   :exception-data d/exception-data
   :http-requests d/http-requests
   :http-responses d/http-responses})

(defn run []
  (let [n 100]
    (with-meta
      (into
       []
       (sort-by
        :cson/read
        (for [[label value] bench-data]
          {:label    label
           :transit/write (:total (b/run :transit (-> value transit/write) n))
           :transit/read  (let [value (transit/write value)]
                            (:total (b/run :transit (-> value transit/read) n)))

           :edn/write     (:total (b/run :edn     (-> value pr-meta) n))
           :edn/read      (let [value (pr-meta value)]
                            (:total (b/run :edn     (-> value edn/read-string) n)))

           :cson/write    (:total (b/run :cson    (-> value cson/write) n))
           :cson/read     (let [value (cson/write value)]
                            (:total (b/run :cson    (-> value cson/read) n)))})))
      {:portal.viewer/default :portal.viewer/table
       :portal.viewer/table
       {:columns
        #?(:cljr    [:label
                     :edn/write :cson/write
                     :edn/read  :cson/read]
           :default [:label
                     :edn/write :transit/write :cson/write
                     :edn/read  :transit/read  :cson/read])}})))
