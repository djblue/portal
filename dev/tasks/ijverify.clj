(ns tasks.ijverify
  #_(:require [tasks.tools :refer [gradle *cwd*]]))

(defn verify
  "Run Intellij Plugin Verification."
  []
  #_(binding [*cwd* "extension-intellij"]
      (gradle "runPluginVerifier"))
  (println "Disabled for now"))
