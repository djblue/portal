(ns tasks.ide
  (:require [tasks.tools :refer [*cwd* gradle]]))

(defn open
  "Open dev extension for intellij."
  []
  (binding [*cwd* "extension-intellij"]
    (gradle "runIde")))