(ns tasks.py
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build]]
            [tasks.tools :refer [*opts* py pip lpy poetry]]))

(defn install []
  (when-not (fs/exists? "target/py")
    (py "-m" :venv "target/py")
    (pip :install "-r" "requirements.txt")))

(defn package []
  (install)
  (build)
  (poetry :build))

(defn deploy  []
  (install)
  (assert (string? (System/getenv "PYPI_TOKEN")) "Need `PYPI_TOKEN` env variable to deploy")
  (with-out-str
    (binding [*opts* {:out *out*}
              *err* *out*]
      (poetry :config :pypi-token.pypi (System/getenv "PYPI_TOKEN"))))
  (poetry :publish))

(defn nrepl [] (lpy :nrepl-server "--include-path" "src"))

(defn -main
  "Start basilisp dev env / nrepl"
  []
  (build)
  (install)
  (nrepl))