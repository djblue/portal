(ns tasks.version
  (:require
   [clojure.string :as str]
   [tasks.info :refer [version]]
   [tasks.tools :refer [git]])
  (:import
   (java.time LocalDateTime)
   (java.time.format DateTimeFormatter)))

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
   "extension-intellij/gradle.properties"
   "extension-vscode/package.json"
   "package.json"
   "src/portal/extensions/vs_code.cljs"
   "src/portal/runtime.cljc"
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

(defn tag
  "Commit and tag a version."
  []
  (set-version version)
  (git :add "-u")
  (git :commit "-m" (str "Release " version))
  (git :tag version))

(defn -main [version] (set-version version))
