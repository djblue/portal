(ns tasks.deploy
  (:require [tasks.ci :refer [ci]]
            [tasks.package :as pkg]
            [tasks.tools :refer [*cwd* npx mvn]]))

(defn- deploy-vscode []
  (binding [*cwd* "extension-vscode"]
    (npx :vsce :publish)))

(defn- deploy-clojars []
  (mvn :deploy))

(defn deploy []
  (pkg/all)
  ;; revert after release
  #_(deploy-clojars)
  (deploy-vscode))

(defn all
  "Deploy all artifacts."
  []
  (ci)
  (deploy))

(defn -main [] (deploy))
