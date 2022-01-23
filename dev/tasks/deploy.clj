(ns tasks.deploy
  (:require [tasks.ci :refer [ci]]
            [tasks.info :refer [version]]
            [tasks.package :as pkg]
            [tasks.tools :refer [*cwd* clj gradle npx]]))

(defn- deploy-vscode []
  (binding [*cwd* "extension-vscode"]
    (npx :vsce :publish)))

(defn- deploy-intellij []
  (binding [*cwd* "extension-intellij"]
    (gradle :publishPlugin)))

(defn- deploy-clojars []
  (clj "-M:deploy" (str "./target/portal-" version ".jar")))

(defn deploy []
  (pkg/all)
  (deploy-clojars)
  (deploy-vscode)
  (deploy-intellij))

(defn all
  "Deploy all artifacts."
  []
  (ci)
  (deploy))

(defn -main [] (deploy))
