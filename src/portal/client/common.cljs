(ns ^:no-doc portal.client.common
  (:require [clojure.string :as str]
            [portal.runtime]
            [portal.runtime.cson :as cson]
            [portal.runtime.json :as json]
            [portal.runtime.transit :as transit]))

(defn- ex-type [ex]
  (let [type-name (some-> ex type .-name)]
    (cond
      (instance? ExceptionInfo ex) 'cljs.core/ExceptionInfo
      (not (str/blank? type-name)) (symbol type-name)
      :else                        'js/Error)))

(defn- error->data [ex]
  (merge
   (when-let [data (ex-data ex)]
     {:data data})
   {:runtime :cljs
    :cause   (ex-message ex)
    :via     [{:type    (ex-type ex)
               :message (ex-message ex)}]
    :stack   (.-stack ex)}))

(defn- serialize [encoding value]
  (try
    (case encoding
      :json    (json/write value)
      :transit (transit/write value)
      :cson    (cson/write value)
      :edn     (binding [*print-meta* true]
                 (pr-str value)))
    (catch :default ex
      (serialize encoding (error->data ex)))))

(defn ->submit [fetch]
  (fn submit
    ([value] (submit nil value))
    ([{:keys [encoding port host]
       :or   {encoding :edn
              host     "localhost"
              port     53755}}
      value]
     (fetch
      (str "http://" host ":" port "/submit")
      {:method "POST"
       :headers
       {"content-type"
        (case encoding
          :json    "application/json"
          :cson    "application/cson"
          :transit "application/transit+json"
          :edn     "application/edn")}
       :body (serialize encoding value)}))))
