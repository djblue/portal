(ns tasks.ide
  (:require [clojure.string :as str]
            [tasks.tools :refer [*cwd* gradle]]))

(def plugins
  {"2021.3" ["com.cursiveclojure.cursive:1.12.2-2021.3" "com.intellij.java"]
   "2022.3" ["com.cursiveclojure.cursive:1.13.1-2022.3" "com.intellij.java"]
   "2023.3" ["com.cursiveclojure.cursive:1.13.1-2023.3" "com.intellij.java"]})

(defn open
  "Open dev extension for intellij."
  [& [version]]
  (binding [*cwd* "extension-intellij"]
    (let [platform (or version "2023.3")]
      (gradle "runIde"
              (str "-PplatformVersion=" platform)
              (str "-PplatformPlugins=" (str/join ", " (get plugins platform)))))))