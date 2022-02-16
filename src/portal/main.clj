(ns portal.main
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [portal.api :as p])
  (:import [java.io PushbackReader]))

(defn- lazy-fn [symbol]
  (fn [& args] (apply (requiring-resolve symbol) args)))

(def ^:private read-json      (lazy-fn 'portal.runtime.json/read-stream))
(def ^:private read-yaml      (lazy-fn 'clj-yaml.core/parse-string))
(def ^:private transit-read   (lazy-fn 'cognitect.transit/read))
(def ^:private transit-reader (lazy-fn 'cognitect.transit/reader))

(defn- read-transit [in]
  (transit-read (transit-reader in :json)))

(defn- read-edn [reader]
  (with-open [in (PushbackReader. reader)] (edn/read in)))

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"     (-> System/in io/reader read-json)
             "yaml"     (-> System/in io/reader slurp read-yaml)
             "edn"      (-> System/in io/reader read-edn)
             "transit"  (-> System/in read-transit))]
    (reset! (p/open) in)
    (.addShutdownHook
     (Runtime/getRuntime)
     (Thread. #(p/close)))
    (println "Press CTRL+C to exit")
    @(promise)))
