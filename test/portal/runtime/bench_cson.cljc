(ns portal.runtime.bench-cson
  (:require [clojure.edn :as edn]
            [examples.data :as d]
            [portal.bench :as b]
            [portal.runtime.cson :as cson]
            [portal.runtime.transit :as transit]
            [portal.viewer :as v]))

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

(defn- pr-meta [v] (binding [*print-meta* true] (pr-str v)))

(defn run-benchmark []
  (let [n 1000 bench-data (assoc bench-data :all bench-data)]
    (concat
     (for [[data value] bench-data
           encoding [:transit :edn :cson]]
       {:time
        (:total
         (case encoding
           :transit (let [value (transit/write value)]
                      (b/run :transit (transit/read value) n))
           :edn     (let [value (pr-meta value)]
                      (b/run :edn     (edn/read-string value) n))
           :cson    (let [value (cson/write value)]
                      (b/run :cson    (cson/read value) n))))
        :test :read
        :encoding encoding
        :data data
        :benchmark (pr-str (keyword (name encoding) "read"))})
     (for [[data value] bench-data
           encoding [:transit :edn :cson]]
       {:time
        (:total
         (case encoding
           :transit (b/run :transit (transit/write value) n)
           :edn     (b/run :edn     (pr-meta value) n)
           :cson    (b/run :cson    (cson/write value) n)))
        :test :write
        :encoding encoding
        :data data
        :benchmark (pr-str (keyword (name encoding) "write"))}))))

(defn charts [data]
  (->> (group-by :data data)
       (sort-by
        (fn [[_ values]]
          (reduce
           +
           (keep
            (fn [{:keys [encoding time]}]
              (when (= :cson encoding)
                time))
            values)))
        >)
       (map
        (fn [[label values]]
          (with-meta
            [:div
             [:h3 {:style {:text-align :center}} label]
             [:portal.viewer/inspector
              (v/vega-lite
               {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
                :data {:values values}
                :mark {:type :bar :tooltip true}
                :encoding
                {:x {:aggregate :sum :field :time}
                 :y {:field :benchmark
                     :type :ordinal
                     :sort {:op :sum :field :time :order :descending}}
                 :color {:field :encoding}}})]]
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
              (for [{:keys [encoding test time]} tests]
                [(keyword (name encoding) (name test)) time]))
        {:portal.viewer/for
         (zipmap (for [{:keys [encoding test]} tests]
                   (keyword (name encoding) (name test)))
                 (repeat :portal.viewer/duration-ms))}))
    (sort-by :cson/read)
    (into []))
   {:columns
    #?(:cljr    [:label
                 :edn/write :cson/write
                 :edn/read  :cson/read]
       :default [:label
                 :edn/write :transit/write :cson/write
                 :edn/read  :transit/read  :cson/read])}))

(defn combined-chart [values]
  (v/vega-lite
   {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
    :data {:values values}
    :mark :bar
    :encoding
    {:x {:field :data
         :sort {:op :sum :field :time :order :descending}}
     :y {:field :time
         :type :quantitative
         :aggregate :sum}
     :xOffset {:field :benchmark
               :sort {:op :sum :field :time :order :descending}}
     :color {:field :benchmark}}}))

(comment
  (def data (doall (run-benchmark)))
  (tap> [data (table data) (charts data) (combined-chart data)])

  (def no-edn (remove (comp #{:edn} :encoding) data))
  (tap> [no-edn (table no-edn) (charts no-edn) (combined-chart no-edn)]))

(defn run [] (table (run-benchmark)))