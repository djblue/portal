(ns portal.ui.viewer.pprint
  (:require [clojure.pprint :as pp]
            [portal.ui.viewer.code :as code]))

(defn pprint-data [value]
  (let [string (with-out-str (pp/pprint value))]
    [code/inspect-code {:class "clojure"} string]))

(def viewer
  {:predicate (constantly true)
   :component pprint-data
   :name :portal.viewer/pprint})
