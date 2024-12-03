(ns tasks.ijverify)

(defn verify
  "Run Intellij Plugin Verification."
  []
  #_(binding [*cwd* "extension-intellij"]
      (gradle "runPluginVerifier"))
  (println "Disabled for now"))
