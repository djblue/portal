(ns version
  (:require [clojure.string :as str])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn- find-version [file-name]
  (first (re-find #"(\d+)\.(\d+)\.(\d+)" (slurp file-name))))

(defn- get-date []
  (.format
   (DateTimeFormatter/ofPattern "yyyy-MM-dd")
   (LocalDateTime/now)))

(defn- changelog [version]
  (let [content (slurp "CHANGELOG.md")]
    (when-not (str/includes? content version)
      {"CHANGELOG.md" (str "## " version " - " (get-date) "\n\n" content)})))

(def files
  ["README.md"
   "pom.xml"
   "src/portal/runtime/index.cljc"])

(defn- version-updates [next-version]
  (let [current-version (find-version "src/portal/runtime/index.cljc")]
    (merge
     (changelog next-version)
     (zipmap
      files
      (for [file files]
        (str/replace (slurp file) current-version next-version))))))

(defn- set-version [version]
  (doseq [[file-name content] (version-updates version)]
    (spit file-name content)))

(defn -main [version] (set-version version))
