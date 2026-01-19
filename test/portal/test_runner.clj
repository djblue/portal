(ns portal.test-runner
  (:require [clojure.test :as t]
            [portal.api :as api]
            [portal.client :as p]
            [portal.client-test]
            [portal.console :as console]
            [portal.runtime-test]
            [portal.runtime.api-test]
            [portal.runtime.cson-test]
            [portal.runtime.edn-test]
            [portal.runtime.fs-test]
            [portal.runtime.json-buffer-test]
            [portal.runtime.jvm.editor-test]
            [portal.runtime.npm-test]
            [portal.runtime.shell-test]))

(defn- run-tests [& tests]
  (if-not (p/enabled?)
    (apply t/run-tests tests)
    (let [report (atom [])
          counts (with-redefs
                  [t/report
                   (fn [message]
                     (swap! report conj
                            (cond-> message
                              :always
                              (assoc :time    (console/now)
                                     :runtime (console/runtime))
                              (:ns message)
                              (update :ns (comp symbol str))))
                     (when (= (:type message) :end-test-ns)
                       (p/submit @report)
                       (reset! report [])))]
                   (add-tap #'p/submit)
                   (apply t/run-tests tests))]
      (remove-tap #'p/submit)
      counts)))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests 'portal.client-test
                   'portal.runtime-test
                   'portal.runtime.api-test
                   'portal.runtime.cson-test
                   'portal.runtime.edn-test
                   'portal.runtime.fs-test
                   'portal.runtime.json-buffer-test
                   'portal.runtime.jvm.editor-test
                   'portal.runtime.npm-test
                   'portal.runtime.shell-test)]
    (api/stop)
    (shutdown-agents)
    (System/exit (+ fail error))))
