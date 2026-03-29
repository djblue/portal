(ns ^:no-doc portal.ui.viewer.pprint
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [portal.ui.filter :as f]
            [portal.ui.inspector :as ins]
            [portal.ui.viewer.code :as code]))

(defn- code? [value]
  (and (or (seq? value) (list? value))
       (symbol? (first value))))

(defn pprint-data [value]
  (let [options (:portal.viewer/pprint (meta value))
        search-text (ins/use-search-text)]
    (binding [*print-meta*   (:print-meta   options (coll? value))
              *print-length* (:print-length options 25)
              *print-level*  (:print-level  options 10)]
      [code/highlight-clj
       (str/trim
        (with-out-str
          (pp/with-pprint-dispatch
            (if (code? value) pp/code-dispatch pp/simple-dispatch)
            (pp/pprint (f/filter-value value search-text)))))])))

(def viewer
  {:predicate (constantly true)
   :component #'pprint-data
   :name :portal.viewer/pprint
   :doc "View value printed via clojure.pprint/pprint."})
