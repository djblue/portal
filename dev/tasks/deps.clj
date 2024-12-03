(ns tasks.deps
  (:require
   [tasks.tools :refer [*cwd* clj git npm]]))

(defn check-deps []
  (npm :outdated)
  (clj "-M:antq" "-m" :antq.core))

(defn assert-clean []
  (git :diff "--exit-code"))

(defn fix-deps
  "Update npm and clj dependencies."
  []
  (assert-clean)
  (npm :update)
  (clj "-M:antq" "-m" :antq.core "--upgrade" "--force")
  (binding [*cwd* "extension-vscode"]
    (npm :update))
  (binding [*cwd* "extension-electron"]
    (npm :update))
  (git :add "-u")
  (git :commit "-m" "Bump deps"))

(defn -main [] (check-deps))
