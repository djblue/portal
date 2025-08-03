(ns tasks.tools
  (:require [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [io.aviso.ansi :as a])
  (:import [java.time Duration]))

(def ^:dynamic *cwd* nil)
(def ^:dynamic *opts* nil)

(defn- now [] (System/currentTimeMillis))

(defn- format-millis [ms]
  (let [duration (Duration/ofMillis ms)
        h        (mod (.toHours duration) 24)
        m        (mod (.toMinutes duration) 60)
        s        (mod (.toSeconds duration) 60)
        ms       (mod ms 1000)]
    (str
     (a/bold-blue "->")
     " "
     (a/bold-yellow
      (str
       (when (> h 0)
         (str h " hours, "))
       (when (> m 0)
         (str m " minutes, "))
       s "." ms " seconds")))))

(def ^:private in-bb? (some? (System/getProperty "babashka.version")))

(def ^:private fns
  (when in-bb? {:clojure (requiring-resolve 'babashka.deps/clojure)}))

(defn- sh* [& args]
  (binding [*out* *err*]
    (println (a/bold-blue "=>")
             (a/bold-green (name (first args)))
             (a/bold-white (str/join " " (map name (rest args))))))
  (let [opts   (merge {:dir      *cwd*
                       :shutdown p/destroy-tree}
                      (merge *opts* (when-not (:inherit *opts*)
                                      {:out *out* :err *err*})))
        start  (now)
        result @(if-let [f (get fns (first args))]
                  (or (f (map name (rest args)) opts)
                      (atom {:exit 0}))
                  (p/process (map name args) opts))
        exit   (:exit result)]
    (when-not (:inherit opts)
      (.flush *out*)
      (.flush *err*))
    (binding [*out* *err*]
      (println
       (str (format-millis (- (now) start))
            (when-not (zero? exit)
              (str " " (a/bold-red (str "(exit: " exit ")")))))))
    (when-not (zero? exit)
      (throw (ex-info (str "Non-zero exit code: "
                           (str/join " " (map name args)))
                      (assoc (select-keys result [:cmd :exit]) :opts *opts*)))))
  true)

(defn sh [& args]
  (if-let [session (:session *opts*)]
    (apply session sh* args)
    (apply sh* args)))

(def ^:private windows?
  (str/starts-with? (System/getProperty "os.name") "Win"))

(defn- local-bin [bin]
  (.getCanonicalPath (io/file (str bin (when windows? ".bat")))))

(def bb     (partial #'sh :bb))
(def nbb    (partial #'sh :npx :nbb))
(def clj    (partial #'sh :clojure))
(def git    (partial #'sh :git))
(def gradle (partial #'sh (local-bin "./extension-intellij/gradlew") "--warning-mode" "all"))
(def node   (partial #'sh :node))
(def npm    (partial #'sh :npm))
(def npx    (partial #'sh :npx))
(def shadow (partial #'clj "-M:cljs:shadow" "-m" "shadow.cljs.devtools.cli"))

(defn cljr [& args]
  (binding [*opts* (assoc-in
                    *opts*
                    [:extra-env "CLOJURE_LOAD_PATH"]
                    (str/join (System/getProperty "path.separator")
                              ["src" "resources" "dev" "test"]))]
    (apply sh :Clojure.Main args)))

(defn py-script [bin]
  (str (if windows?
         "./target/py/Scripts/"
         "./target/py/bin/")
       (name bin)))

(def py  (partial #'sh :python3))
(def pip (partial #'sh (py-script :pip)))
(def poetry (partial #'sh (py-script :poetry)))

(defn lpy [& args]
  (binding [*opts*
            (assoc *opts*
                   :inherit true
                   :extra-env
                   {"PYTHONPATH" "src:test"})]
    (apply sh (py-script :basilisp) args)))

(defn cljs [version main]
  (let [deps {'org.clojure/clojurescript {:mvn/version version}}
        out  (str "target/" (name main) "." version)]
    (when (seq
           (fs/modified-since
            out
            (concat
             (fs/glob "src" "**")
             (fs/glob "test" "**"))))
      (clj "-Sdeps" (pr-str {:deps deps})
           "-M:test"
           "-m" :cljs.main
           "--output-dir" out
           "--target" :node
           "--output-to" (str out ".js")
           "--compile" main))
    (node out)))