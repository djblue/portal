(ns portal.test-runtime-runner
  (:require [clojure.test :as t]
            [portal.runtime.api-test]
            [portal.runtime.bench-cson :as bench]
            [portal.runtime.cson-test]
            [portal.runtime.edn-test]
            [portal.runtime.fs :as fs]
            [portal.runtime.fs-test]
            [portal.runtime.json :as json]
            [portal.runtime.json-buffer-test]
            [portal.runtime.npm-test]
            [portal.test-runner :as runner]))

(defn -main []
  (runner/run
   #(t/run-tests 'portal.runtime.api-test
                 'portal.runtime.cson-test
                 'portal.runtime.edn-test
                 'portal.runtime.fs-test
                 'portal.runtime.json-buffer-test
                 'portal.runtime.npm-test))
  (runner/table (bench/run (json/read (fs/slurp "package-lock.json")) 100)))

(-main)
