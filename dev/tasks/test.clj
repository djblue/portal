(ns tasks.test
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [tasks.build :refer [build install]]
            [tasks.py :as py]
            [tasks.tools :as t]))

(defn cljs* [deps main]
  (let [version (get-in deps ['org.clojure/clojurescript :mvn/version])
        out     (str "target/" (name main) "." version ".js")]
    (when (seq
           (fs/modified-since
            out
            (concat
             (fs/glob "src" "**")
             (fs/glob "test" "**"))))
      (t/clj "-Sdeps" (pr-str {:deps deps})
             "-M:test"
             "-m" :cljs.main
             "--output-dir" (str "target/cljs-output-" version)
             "--target" :node
             "--output-to" out
             "--compile" main))
    (t/node out)))

(defn cljs-runtime [version]
  (cljs* {'org.clojure/clojurescript {:mvn/version version}} :portal.test-runtime-runner))

(defn- get-cljs-deps []
  (get-in (read-string (slurp "deps.edn")) [:aliases :cljs :extra-deps]))

(defn cljs-ui []
  (install)
  (cljs* (get-cljs-deps) :portal.test-ui-runner))

(defn cljs-nbb []
  (t/nbb "-m" :portal.test-runtime-runner))

(defn- setup* [version]
  (t/clj
   "-Sforce" "-Spath" "-Sdeps"
   (pr-str {:deps {'org.clojure/clojurescript {:mvn/version version}}})))

(defn setup []
  (setup* "1.10.773")
  (setup* "1.10.844"))

(defn cljs []
  (build)
  (cljs-runtime "1.10.773")
  (cljs-runtime "1.10.844")
  (cljs-nbb))

(defn clj
  []
  (build)
  (t/clj "-M:test" "-m" :portal.test-runner)
  (t/bb "-m" :portal.test-runner))

(defn cljr []
  (build)
  (t/cljr "-m" :portal.test-clr))

(defn lpy []
  (py/install)
  (t/lpy :run "--include-path" "test" "--include-path" "src" "-n" :portal.test-runner))

(defn test* []
  (future (cljs-runtime "1.10.773"))
  (future (cljs-runtime "1.10.844"))
  (future
    (build)
    (future (t/clj "-M:test" "-m" :portal.test-runner))
    (future (t/bb "-m" :portal.test-runner))
    (future (cljr))))

(defn test "Run all clj/s tests." [] (cljs) (cljs-ui) (clj))

(defn -main [] (test))
