(ns tasks.tools
  (:require [babashka.deps :as deps]
            [babashka.process :as p]
            [clojure.string :as str]
            [io.aviso.ansi :as a])
  (:import [java.time Duration]))

(def ^:dynamic *cwd* nil)

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
         (str h " minutes, "))
       s "." ms " seconds")))))

(defn sh [& args]
  (println (a/bold-blue "=>")
           (a/bold-green (name (first args)))
           (a/bold (str/join " " (map name (rest args)))))
  (let [opts {:inherit true :dir *cwd*}
        start (now)
        ps (if-not (= (first args) :clojure)
             (p/process (map name args) opts)
             (deps/clojure (map name (rest args)) opts))]
    (p/check ps)
    (println (format-millis (- (now) start)) "\n"))
  nil)

(def bb     (partial sh :bb))
(def clj    (partial sh :clojure))
(def git    (partial sh :git))
(def gradle (partial sh "./gradlew" "--warning-mode" "all"))
(def node   (partial sh :node))
(def npm    (partial sh :npm))
(def npx    (partial sh :npx))
(def shadow (partial clj "-M:cljs:shadow" "-m" "shadow.cljs.devtools.cli"))
