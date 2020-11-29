(ns portal.runtime.transit
  (:require [cognitect.transit :as t]
            [portal.runtime :as rt]))

(defn- find-var [s] (get @rt/instance-cache [:var s]))

(defn json->edn [json]
  (t/read
   (t/reader
    :json
    {:handlers
     {"r"
      (t/read-handler #(js/URL. %))
      "portal.transit/var"
      (t/read-handler
       (fn [s] (let [[_ pair] s] (find-var (first pair)))))
      "portal.transit/object" (t/read-handler (comp rt/uuid->instance :id))}})
   json))

(defn var->symbol [v]
  (let [m (meta v)]
    (symbol (str (:ns m)) (str (:name m)))))

;; Using the normal t/write-meta causes an issue with vars. Since
;; transit-cljs seems to process metadata before custom handlers, the
;; metadata is stripped from the var, rendering the handler useless.
;; Moreover, metadata is then associated with the tagged value, resultsing
;; in issues for consumers because Transit$TaggedValue doesn't implement
;; any metadata protocols. I don't know if this is inteded behavior or a
;; bug, it seems to work without issue for transit-clj.

(defn write-meta
  "For :transform. Will write any metadata present on the value.
  Forked from https://github.com/cognitect/transit-cljs/blob/master/src/cognitect/transit.cljs#L412"
  [x]
  (cond
    (instance? cljs.core/Var x)
    (let [s (var->symbol x)]
      (swap! rt/instance-cache assoc [:var s] x)
      (t/tagged-value "portal.transit/var" (with-meta s (meta x))))

    (implements? IMeta x)
    (if-let [m (-meta ^not-native x)]
      (t/WithMeta. (-with-meta ^not-native x nil) m)
      x)

    :else x))

(defn edn->json [edn]
  (t/write
   (t/writer
    :json
    {:transform (comp write-meta rt/limit-seq)
     :handlers
     {js/URL
      (t/write-handler (constantly "r") str)
      :default
      (t/write-handler (constantly "portal.transit/object") rt/object->value)}})
   edn))
