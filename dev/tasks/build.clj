(ns tasks.build
  (:require [babashka.fs :as fs]
            [tasks.info :refer [version]]
            [tasks.tools :refer [*cwd* gradle npm npx shadow]]))

(defn install []
  (when (seq
         (fs/modified-since
          "node_modules"
          ["package.json" "package-lock.json"]))
    (npm :ci)))

(defn main-js []
  (install)
  (when (seq
         (fs/modified-since
          "resources/portal/main.js"
          (concat
           ["deps.edn"
            "package.json"
            "package-lock.json"
            "shadow-cljs.edn"]
           (fs/glob "src/portal/ui" "**.cljs"))))
    (shadow :release :client))
  (fs/copy "resources/icon.svg"
           "resources/portal/icon.svg"
           {:replace-existing true}))

(defn ws-js []
  (install)
  (let [out "resources/portal/ws.js"]
    (when (seq
           (fs/modified-since
            out
            ["package.json" "package-lock.json"]))
      (npx :browserify
           "--node"
           "--exclude" :bufferutil
           "--exclude" :utf-8-validate
           "--standalone" :Server
           "--outfile" out
           "node_modules/ws"))))

(defn build [] (main-js) (ws-js))

(defn prep [_] (install) (build) (shutdown-agents))

(defn vs-code-extension
  "Build vs-code extension."
  []
  (build)
  (shadow :release :vs-code :vs-code-notebook)
  (binding [*cwd* "extension-vscode"]
    (npm :ci)
    (fs/copy "README.md" "extension-vscode/" {:replace-existing true})
    (fs/copy "LICENSE" "extension-vscode/LICENSE" {:replace-existing true})
    (npx :vsce :package)))

(defn intellij-extension
  "Build intellij extension."
  []
  (binding [*cwd* "extension-intellij"]
    (gradle "buildPlugin")
    (println (str "extension-intellij/build/distributions/portal-extension-intellij-" version ".zip"))))

(defn extensions
  "Build editor extensions."
  []
  (vs-code-extension)
  (intellij-extension))
