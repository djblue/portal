(ns portal.main
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [portal.http-socket-server :as http]
            [portal.runtime :as rt]
            [portal.runtime.transit :as t]
            [portal.runtime.launcher :as l])
  (:import [java.io PushbackReader]))

(defn read-edn [reader]
  (with-open [in (PushbackReader. reader)] (edn/read in)))

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"     (-> System/in io/reader (json/parse-stream true))
             "edn"      (-> System/in io/reader read-edn)
             "transit"  (-> System/in t/json-stream->edn))]
    (rt/update-value in)
    (http/wait (l/open nil))
    (shutdown-agents)))
