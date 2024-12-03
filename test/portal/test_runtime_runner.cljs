(ns portal.test-runtime-runner
  (:require
   [clojure.string :as str]
   [clojure.test :as t]
   [portal.client-test]
   [portal.runtime-test]
   [portal.runtime.api-test]
   [portal.runtime.bench-cson :as bench]
   [portal.runtime.cson-test]
   [portal.runtime.edn-test]
   [portal.runtime.fs-test]
   [portal.runtime.json-buffer-test]
   [portal.runtime.npm-test]
   [portal.runtime.shell-test]
   [portal.test-runner :as runner]))

(defn -main []
  (runner/run
    #(t/run-tests 'portal.client-test
                  'portal.runtime-test
                  'portal.runtime.api-test
                  'portal.runtime.cson-test
                  'portal.runtime.edn-test
                  'portal.runtime.fs-test
                  'portal.runtime.json-buffer-test
                  'portal.runtime.npm-test
                  'portal.runtime.shell-test))
  (runner/table (bench/run)))

(when-not (str/ends-with? (second js/process.argv) "nbb")
  (-main))
