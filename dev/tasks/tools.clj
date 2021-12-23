(ns tasks.tools
  (:require [babashka.deps :as deps]
            [babashka.process :as p]
            [clojure.string :as str]
            [io.aviso.ansi :as a]))

(def ^:dynamic *cwd* nil)

(defn sh [& args]
  (println (a/bold-blue "=>")
           (a/bold-green (name (first args)))
           (a/bold (str/join " " (map name (rest args)))))
  (let [opts {:inherit true :dir *cwd*}
        ps (if-not (= (first args) :clojure)
             (p/process (map name args) opts)
             (deps/clojure (map name (rest args)) opts))]
    (p/check ps))
  nil)

(def bb     (partial sh :bb))
(def clj    (partial sh :clojure))
(def git    (partial sh :git))
(def gradle (partial sh "./gradlew" "--warning-mode" "all"))
(def mvn    (partial sh :mvn))
(def node   (partial sh :node))
(def npm    (partial sh :npm))
(def npx    (partial sh :npx))
(def shadow (partial clj "-M:cljs:shadow"))
