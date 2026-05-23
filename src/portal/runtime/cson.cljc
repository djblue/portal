(ns ^:no-doc portal.runtime.cson
  "Clojure/Script Object Notation"
  (:refer-clojure :exclude [read])
  (:require
   [portal.runtime.cson.reader :as reader]
   [portal.runtime.cson.writer :as writer]))

(defn write
  ([value]
   (writer/write value nil))
  ([value options]
   (writer/write value options)))

(defn read
  ([string]
   (reader/read string nil))
  ([string options]
   (reader/read string options)))