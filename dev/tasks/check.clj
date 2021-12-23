(ns tasks.check
  (:require [tasks.format :as fmt]
            [tasks.tools :refer [clj gradle *cwd*]]))

(defn check
  "Run all static analysis checks."
  []
  (fmt/check)
  (clj "-M:kondo"
       "--lint" :dev :src :test
       "extension-intellij/src/main/clojure")
  (clj "-M:cider:check")
  (binding [*cwd* "extension-intellij"]
    (gradle "checkClojure")))

(defn -main [] (check))
