(ns tasks.jank
  (:require
   [babashka.fs :as fs]
   [portal.api :as p]
   [tasks.tools :refer [*opts* sh]]))

(defn- jank-env []
  (let [cwd (fs/cwd)
        jank-dir (fs/canonicalize (fs/path cwd ".." "jank" "compiler+runtime"))
        jank-local-llvm-bin (fs/path jank-dir "build/llvm-install/usr/local/bin")]
    [(str (fs/file jank-dir "build" "jank"))
     {"CXX" (str (fs/file jank-local-llvm-bin "clang++"))
      "CC"  (str (fs/file jank-local-llvm-bin "clang"))}]))

(defn- nrepl [env]
  (let [[bin extra-env] (jank-env)]
    (binding [*opts* {:inherit true :extra-env (merge env extra-env)}]
      (sh bin :repl
          "--module-path" "src:dev:test"
          "-I/usr/include" "-L/usr/lib" "-lcurl"))))

(defn -main
  "Start jank dev env / nrepl"
  []
  (let [server (p/start {})]
    (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (p/close))))
    (p/open {:launcher :auto})
    (nrepl {"PORTAL_PORT" (:port server)})))