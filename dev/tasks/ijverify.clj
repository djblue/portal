(ns tasks.ijverify
  (:require [tasks.tools :refer [gradle *cwd*]]))

(defn verify
  "Run Intellij Plugin Verification."
  []
  (binding [*cwd* "extension-intellij"]
    (gradle "runPluginVerifier")))
