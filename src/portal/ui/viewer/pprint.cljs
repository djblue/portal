(ns ^:no-doc portal.ui.viewer.pprint
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [portal.runtime.cson :as cson]
            [portal.ui.filter :as f]
            [portal.ui.inspector :as ins]
            [portal.ui.viewer.code :as code]))

(defn- queue? [obj]
  (instance? PersistentQueue obj))

(defn- deref? [obj]
  (satisfies? IDeref obj))

(defn- type-dispatcher [obj]
  (cond
    (cson/tagged-value? obj) :tagged
    (ins/bin? obj) :bin

    (queue? obj)  :queue
    (deref? obj)  :deref
    (symbol? obj) :symbol
    (seq? obj)    :list
    (map? obj)    :map
    (vector? obj) :vector
    (set? obj)    :set
    (nil? obj)    nil
    :else         :default))

(defmulti pprint-dispatch type-dispatcher)

(def ^:dynamic *elide-binary* false)

(when (exists? js/Uint8Array)
  (extend-type js/Uint8Array
    IPrintWithWriter
    (-pr-writer [^js/Uint8Array this writer _opts]
      (-write writer "#object[Uint8Array ")
      (-write writer (.-length this))
      (-write writer "]"))))

(defmethod pprint-dispatch :bin [value]
  (if *elide-binary*
    (-write *out* (pr-str value))
    (do
      (-write *out* "#portal/bin \"")
      (-write *out* (cson/base64-encode value))
      (-write *out* "\""))))

(defmethod pprint-dispatch :tagged  [value] (-write *out* (pr-str value)))
(defmethod pprint-dispatch :list    [value] (#'pp/pprint-list value))
(defmethod pprint-dispatch :vector  [value] (#'pp/pprint-vector value))
(defmethod pprint-dispatch :map     [value] (#'pp/pprint-map value))
(defmethod pprint-dispatch :set     [value] (#'pp/pprint-set value))
(defmethod pprint-dispatch :queue   [value] (#'pp/pprint-pqueue value))
(defmethod pprint-dispatch nil      [_]     (-write *out* "nil"))
(defmethod pprint-dispatch :default [value] (#'pp/pprint-simple-default value))

(pp/set-pprint-dispatch pprint-dispatch)

(defn- code? [value]
  (and (or (seq? value) (list? value))
       (symbol? (first value))))

(defn pprint-data [value]
  (let [options (:portal.viewer/pprint (meta value))
        search-text (ins/use-search-text)]
    (binding [*print-meta*   (:print-meta   options (coll? value))
              *print-length* (:print-length options 25)
              *print-level*  (:print-level  options 10)
              *elide-binary* true]
      [code/highlight-clj
       (str/trim
        (with-out-str
          (pp/with-pprint-dispatch (if (code? value) pp/code-dispatch pprint-dispatch)
            (pp/pprint (f/filter-value value search-text)))))])))

(def viewer
  {:predicate (constantly true)
   :component #'pprint-data
   :name :portal.viewer/pprint
   :doc "View value printed via clojure.pprint/pprint."})
