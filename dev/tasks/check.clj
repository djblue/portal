(ns tasks.check
  (:require [portal.runtime.json :as json]
            [tasks.format :as fmt]
            [tasks.tools :refer [sh clj gradle *cwd*]]))

(defn cloc []
  (-> (sh :cloc "--json" :src :dev :test)
      (with-out-str)
      (json/read)
      (dissoc :header)
      (with-meta
        {:portal.viewer/default :portal.viewer/table
         :portal.viewer/table
         {:columns [:nFiles :blank :comment :code]}})))

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
