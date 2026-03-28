(ns portal.test-runner
  (:require [clojure.test :as t]
            [portal.api :as api]
            [portal.client :as p]
            [portal.console :as console]))

(defn- run-tests [& tests]
  (let [tests (remove nil? tests)]
    (doseq [test tests] (require test))
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
        counts))))

(def ^:private in-bb? (some? (System/getProperty "babashka.version")))

(defn -main []
  (let [{:keys [fail error]}
        (run-tests 'portal.client-test
                   (when-not in-bb? 'portal.nrepl-test)
                   'portal.runtime-test
                   'portal.runtime.api-test
                   'portal.runtime.cson-test
                   'portal.runtime.edn-test
                   'portal.runtime.fs-test
                   'portal.runtime.json-buffer-test
                   (when-not in-bb?
                     'portal.runtime.jvm.custom-types-test)
                   'portal.runtime.jvm.editor-test
                   'portal.runtime.npm-test
                   'portal.runtime.shell-test
                   'portal.ssr.ui.react-test
                   'portal.ui.state-test)]
    (api/stop)
    (shutdown-agents)
    (System/exit (+ fail error))))
