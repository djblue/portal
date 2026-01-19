(ns tasks.ci
  (:require [tasks.check :refer [check]]
            [tasks.test :refer [test] :as test]
            [tasks.tools :refer [clj]]))

(def ^:private commands
  ["-M:cljfmt"
   "-M:cljs"
   "-M:cljs:shadow"
   "-M:dev"
   "-M:kondo"
   "-M:test"
   "-X:deploy"])

(defn setup []
  (test/setup)
  (doseq [command commands] (clj "-Sforce" "-Spath" command)))

(defn ci
  "Run all CI Checks."
  []
  (check) (test))

(defn -main [] (ci))