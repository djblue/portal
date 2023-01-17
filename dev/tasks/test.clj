(ns tasks.test
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build]]
            [tasks.tools :as t]))

(defn- cljs* [version]
  (let [out (str "target/test." version ".js")]
    (when (seq
           (fs/modified-since
            out
            (concat
             (fs/glob "src" "**")
             (fs/glob "test" "**"))))
      (t/clj "-Sdeps"
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
    (t/node out)))

(defn cljs []
  (cljs* "1.10.773")
  (cljs* "1.10.844"))

(defn clj
  []
  (build)
  (t/clj "-M:test" "-m" :portal.test-runner)
  (t/bb "-m" :portal.test-runner))

(defn cljr []
  (binding [t/*opts* (assoc-in t/*opts* [:extra-env "CLOJURE_LOAD_PATH"] "src:resources:test")]
    (t/cljr "-m" :portal.test-clr)))

(defn test* []
  (future (cljs* "1.10.773"))
  (future (cljs* "1.10.844"))
  (future
    (build)
    (future (t/clj "-M:test" "-m" :portal.test-runner))
    (future (t/bb "-m" :portal.test-runner))))

(defn test "Run all clj/s tests." [] (cljs) (clj))

(defn -main [] (test))
