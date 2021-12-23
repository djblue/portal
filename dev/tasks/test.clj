(ns tasks.test
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build]]
            [tasks.tools :refer [bb clj node sh]]))

(defn test-cljs [version]
  (let [out (str "target/test." version ".js")]
    (when (seq
           (fs/modified-since
            out
            (concat
             (fs/glob "src" "**")
             (fs/glob "test" "**"))))
      (clj "-Sdeps"
           (pr-str
            {:deps
             {'org.clojure/clojurescript
              {:mvn/version version}}})
           "-M:test"
           "-m" :cljs.main
           "--output-dir" (str "target/cljs-output-" version)
           "--target" :node
           "--output-to" out
           "--compile" :portal.test-runner))
    (node out)))

(defn test
  "Run all clj/s tests."
  []
  (test-cljs "1.10.773")
  (test-cljs "1.10.844")
  (sh :planck "-c" "src:test" "-m" :portal.test-planck)
  (build)
  (clj "-M:test" "-m" :portal.test-runner)
  (bb "-m" :portal.test-runner))

(defn -main [] (test))
