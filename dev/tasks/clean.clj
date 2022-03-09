(ns tasks.clean
  (:require [babashka.fs :as fs]
            [tasks.info :refer [version]]))

(defn rm [path]
  (when (fs/exists? path)
    (println "=>" "rm" path)
    (fs/delete-tree path))
  nil)

(def ^:private clean-files
  ["extension-intellij/build/"
   "extension-vscode/vs-code.js"
   "extension-vscode/vs-code.js.map"
   "pom.xml"
   "resources/portal/"
   "resources/portal-dev/"
   "target/"
   (str "extension-vscode/portal-" version ".vsix")])

(defn clean
  "Remove target and resources/portal"
  []
  (doseq [file clean-files] (rm file)))

(defn -main [] (clean))
