(ns portal.main
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [portal.api :as p]
            [portal.runtime.transit :as t])
  (:import [java.io PushbackReader]))

(defn read-edn [reader]
  (with-open [in (PushbackReader. reader)] (edn/read in)))

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"     (-> System/in io/reader (json/parse-stream true))
             "edn"      (-> System/in io/reader read-edn)
             "transit"  (-> System/in t/json-stream->edn))]
    (p/open)
    (p/tap)
    (tap> in)
    (println "Press CTRL+C to exit")
    @(promise)))
