(ns tasks.jank
  (:require
   [clojure.java.io :as io]
   [org.httpkit.client :as http]
   [tasks.tools :refer [jank]])
  (:import [java.security MessageDigest]))

(defn- sha256 [^bytes data]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") data)]
    (apply str (map #(format "%02x" %) digest))))

(def ^:private deps
  '{nlohmann/json
    {:version "v3.12.0"
     :url "https://github.com/nlohmann/json/releases/download/v3.12.0/json.hpp"
     :sha256 "aaf127c04cb31c406e5b04a63f1ae89369fccde6d8fa7cdda1ed4f32dfc5de63"
     :target "nlohmann/json.hpp"}
    yhirose/cpp-httplib
    {:version "v0.60.1"
     :url "https://raw.githubusercontent.com/yhirose/cpp-httplib/refs/tags/v0.60.1/httplib.h"
     :sha256 "e932d7771bdb93150868987e30112d9a31e24bef592d6d7a07e973d5845cf8d6"
     :target "httplib.h"}})

(defn install []
  (println "=> Install jank deps")
  (doseq [[dep {:keys [url target] expected :sha256}] deps
          :let [target (io/file ".portal/include" target)]
          :when (not (.exists target))
          :let [_ (println " -> " url)
                {:keys [body]} @(http/request {:method :get :url url :as :byte-array})
                actual (sha256 body)]]
    (when-not (= expected actual)
      (throw (ex-info
              "Download failed sha256 check."
              {:dep dep :expected expected :actual actual})))
    (.mkdirs (.getParentFile target))
    (io/copy body target)))

(defn -main
  "Start jank dev env / nrepl"
  []
  (install)
  (jank "--module-path" "src:resources:dev:test" :repl))