(ns tasks.py
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build]]
            [tasks.tools :refer [py pip lpy poetry]]))

(defn install []
  (when-not (fs/exists? "target/py")
    (py "-m" :venv "target/py")
    (pip :install "-r" "requirements.txt")))

(defn package [] (install) (poetry :build))
(defn deploy  [] (install) (poetry :publish))

(defn nrepl [] (lpy :nrepl-server "--include-path" "src"))

(defn -main
  "Start basilisp dev env / nrepl"
  []
  (build)
  (install)
  (nrepl))