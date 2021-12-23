(ns tasks.deploy
  (:require [tasks.ci :refer [ci]]
            [tasks.package :as pkg]
            [tasks.tools :refer [*cwd* npx mvn]]))

(defn- deploy-vscode []
  (binding [*cwd* "extension-vscode"]
    (npx :vsce :publish)))

(defn- deploy-clojars []
  (mvn :deploy))

(defn all
  "Deploy all artifacts."
  []
  (ci)
  (pkg/all)
  (deploy-clojars)
  (deploy-vscode))
