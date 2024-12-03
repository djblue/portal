(ns tasks.format
  (:require
   [tasks.tools :refer [npx]]))

(defn check []
  (npx "@chrisoakman/standard-clojure-style" :check
       :dev :src :test
       "extension-intellij/src/main/clojure"))

(defn fix
  "Run cljfmt formatter."
  []
  (npx "@chrisoakman/standard-clojure-style" :fix
       :dev :src :test
       "extension-intellij/src/main/clojure"))
