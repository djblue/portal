(ns portal.test-ui-runner
  (:require
   [clojure.test :as t]
   [portal.runtime.cson-test]
   [portal.runtime.edn-test]
   [portal.runtime.json-buffer-test]
   [portal.test-runner :as runner]
   [portal.ui.state-test]))

(defn -main []
  (runner/run
    #(t/run-tests 'portal.runtime.cson-test
                  'portal.runtime.edn-test
                  'portal.runtime.json-buffer-test
                  'portal.ui.state-test)))

(-main)
