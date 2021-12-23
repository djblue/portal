(ns tasks.format
  (:require [tasks.tools :refer [clj]]))

(defn check []
  (clj "-M:cljfmt" :check
       :dev :src :test
       "extension-intellij/src/main/clojure"))

(defn fix
  "Run cljfmt formatter."
  []
  (clj "-M:cljfmt" :fix
       :dev :src :test
       "extension-intellij/src/main/clojure"))
