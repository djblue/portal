(ns tasks.coverage
  (:require [cloverage.coverage :as c]
            [portal.test-runner :as t]))

(defmethod c/runner-fn ::runner [_]
  (fn [_nses] {:errors (t/runner)}))

(defn -main []
  (c/run-project
   {:test-ns-path ["test/portal"]
    :src-ns-path ["src/portal"]
    :ns-exclude-regex [#".*clr.*" #"portal\.ui\..*"]
    :runner ::runner
    :runner-opts {}}))