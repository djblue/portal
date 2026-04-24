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
    (binding [*opts* {:inherit true
                      :extra-env
                      (merge env extra-env
                             {"LIBGL_ALWAYS_SOFTWARE" "1"
                              "GLFW_USE_WAYLAND" "0"
                              ;; "GLFW_PLATFORM" "x11"
                              })}]

      (sh bin :repl
          "--module-path" "src:dev:test"
          "-I/usr/include"

         "-I/usr/include/pango-1.0"
         "-I/usr/include/cairo"
          "-I/usr/include/pixman-1"
          "-I/usr/include/libmount"
          "-I/usr/include/blkid"
          "-I/usr/include/fribidi"
          "-I/usr/include/harfbuzz"
          "-I/usr/include/freetype2"
          "-I/usr/include/libpng16"
          "-I/usr/include/glib-2.0"
          "-I/usr/lib/glib-2.0/include"
          "-I/usr/include/sysprof-6"
          ;; "-pthread"

          "-L/usr/lib"
          "-lcurl"
          "-lglfw"
          "-lcairo"
          "-lpango-1.0"
          "-lpangocairo-1.0"
          "-lgobject-2.0"))))

(defn -main
  "Start jank dev env / nrepl"
  []
  (let [server (p/start {})]
    (.addShutdownHook (Runtime/getRuntime) (Thread. (fn [] (p/close))))
    (p/open {:launcher :auto})
    (nrepl {"PORTAL_PORT" (:port server)})))