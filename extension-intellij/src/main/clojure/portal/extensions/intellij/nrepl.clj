(ns portal.extensions.intellij.nrepl
  (:require [nrepl.server :as nrepl-server]))

(defn run [] (nrepl-server/start-server :port 7888))