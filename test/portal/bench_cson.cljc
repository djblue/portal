(ns portal.bench-cson
  (:require [clojure.edn :as edn]
            [examples.data :as d]
            [portal.bench :as b]
            [portal.client :as p]
            [portal.console :as console]
            [portal.runtime.cson :as cson]
            [portal.runtime.cson.writer-simple :as cws]
            [portal.runtime.transit :as transit]
            [portal.viewer :as v]))

(def bench-data
  {:platform-data (select-keys d/platform-data [::d/uuid ::d/date])
   :basic-data (dissoc d/basic-data ::d/range)
   :hiccup-data d/hiccup
   ;; TODO: re-enable when chunked seqs get fixed in jank
  ;;  :data-visualization d/data-visualization
   :string-data d/string-data
   :log-data d/log-data
   :profile-data d/profile-data
   :prepl-data d/prepl-data
   :exception-data d/exception-data
   :http-requests d/http-requests
   :http-responses d/http-responses})

(defn- pr-meta [v]
  (binding [*print-meta* true]
    (pr-str v)))

(def ^:private formats
  #?(:org.babashka/nbb [#_:edn :cson #_:cson-simple]
     :cljr [#_:edn :cson #_:cson-simple]
     :lpy [:cson #_:cson-simple]
     :jank [:cson]
     :default [#_:transit #_:edn :cson #_:cson-simple]))

(defn run-benchmark []
  (doall
   (let [n 10 bench-data (assoc bench-data :all bench-data)]
     (concat
      (for [[data value] bench-data
            encoding formats
            :when (not= encoding :cson-simple)]
        (merge
         (case encoding
           :transit (let [value (transit/write value)]
                      (b/run (transit/read value) n))
           :edn     (let [value (pr-meta value)]
                      (try
                        (b/run (edn/read-string value) n)
                        (catch #?(:cljs :default :jank jank.runtime.object_ref :default Exception) e
                          (tap> e)
                          (throw e))))
           :cson    (let [value (cson/write value)]
                      (b/run (cson/read value) n)))
         {:test :read
          :encoding encoding
          :data data
          :runtime (console/runtime)
          :benchmark (pr-str (keyword (name encoding) "read"))}))
      (for [[data value] bench-data
            encoding formats]
        (merge
         (case encoding
           :transit (b/run (transit/write value) n)
           :edn     (b/run (pr-meta value) n)
           :cson    (b/run (cson/write value) n)
           :cson-simple (b/run (cws/write value) n))
         {:test :write
          :encoding (if (= :cson-simple encoding) :cson encoding)
          :data data
          :runtime (console/runtime)
          :benchmark (pr-str (keyword (name encoding) "write"))}))))))

(defn charts [data]
  (->> data
       (map (fn [{:keys [benchmark runtime] :as x}]
              (assoc x :benchmark (str (name runtime) "/" benchmark))))
       (group-by :data)
       (sort-by
        (fn [[_ values]]
          (reduce
           +
           (keep
            (fn [{:keys [encoding total]}]
              (when (= :cson encoding)
                total))
            values)))
        >)
       (map
        (fn [[label values]]
          (with-meta
            [:div
             [:h3 {:style {:text-align :center}} label]
             [:portal.viewer/inspector
              (-> {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                   :data {:values values}
                   :mark {:type :bar :tooltip true}
                   :encoding
                   {:x {:aggregate :sum :field :total}
                    :y {:field :benchmark
                        :type :ordinal
                        :sort {:op :sum :field :total :order :descending}}
                    :color {:field :runtime}}}
                  (v/vega-lite)
                  (vary-meta assoc :value (get bench-data label bench-data)))]]
            {:key (str label)})))
       (into [:div
              {:style {:display :grid
                       :gap 20
                       :grid-template-columns "auto auto auto auto"}}])
       v/hiccup))

(defn table [data]
  (v/table
   (->>
    (for [[label tests] (group-by :data data)]
      (with-meta
        (into {:label label}
              (for [{:keys [encoding test total]} tests]
                [(keyword (name encoding) (name test)) total]))
        {:portal.viewer/for
         (zipmap (for [{:keys [encoding test]} tests]
                   (keyword (name encoding) (name test)))
                 (repeat :portal.viewer/duration-ms))}))
    (sort-by :cson/read)
    (into []))
   {:columns
    #?(:cljr
       [:label
        :edn/write :cson/write
        :edn/read  :cson/read]
       :org.babashka/nbb
       [:label
        :edn/write :cson/write
        :edn/read  :cson/read]
       :default
       [:label
        :edn/write :transit/write :cson/write
        :edn/read  :transit/read  :cson/read])}))

(defn combined-chart [values]
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:values values}
    :mark :bar
    :encoding
    {:x {:field :data
         :sort {:op :sum :field :total :order :descending}}
     :y {:field :total
         :type :quantitative
         :aggregate :sum}
     :xOffset {:field :benchmark
               :sort {:op :sum :field :total :order :descending}}
     :color {:field :benchmark}}}))

(defn run [] (table (run-benchmark)))

(defn -main [] (p/submit (run-benchmark)))

#?(:lpy (-main) :org.babashka/nbb :skip :cljs (-main) :jank (-main))