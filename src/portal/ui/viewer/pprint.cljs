(ns portal.ui.viewer.pprint
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [portal.runtime.cson :as cson]
            [portal.ui.viewer.code :as code]))

(defn- queue? [obj]
  (instance? PersistentQueue obj))

(defn- deref? [obj]
  (satisfies? IDeref obj))

(defn- type-dispatcher [obj]
  (cond
    (cson/tagged-value? obj) :tagged

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

(defmethod pprint-dispatch :tagged  [value] (-write *out* (:rep value)))
(defmethod pprint-dispatch :list    [value] (#'pp/pprint-list value))
(defmethod pprint-dispatch :vector  [value] (#'pp/pprint-vector value))
(defmethod pprint-dispatch :map     [value] (#'pp/pprint-map value))
(defmethod pprint-dispatch :set     [value] (#'pp/pprint-set value))
(defmethod pprint-dispatch :queue   [value] (#'pp/pprint-pqueue value))
(defmethod pprint-dispatch :deref   [value] (#'pp/pprint-ideref value))
(defmethod pprint-dispatch nil      [_]     (-write *out* "nil"))
(defmethod pprint-dispatch :default [value] (#'pp/pprint-simple-default value))

(pp/set-pprint-dispatch pprint-dispatch)

(defn pprint-data [value]
  (let [string (str/trim (with-out-str (pp/pprint value)))]
    [code/inspect-code {:class "clojure"} string]))

(def viewer
  {:predicate (constantly true)
   :component pprint-data
   :name :portal.viewer/pprint})
