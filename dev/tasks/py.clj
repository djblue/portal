(ns tasks.py
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build]]
            [tasks.tools :refer [py pip lpy]]))

(defn install []
  (when-not (fs/exists? "target/py")
    (py "-m" :venv "target/py")
    (pip :install "-r" "requirements.txt")))

(defn nrepl [] (lpy :nrepl-server))

(defn -main
  "Start basilisp dev env / nrepl"
  []
  (build)
  (install)
  (nrepl))