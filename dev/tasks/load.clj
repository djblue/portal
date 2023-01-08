(ns tasks.load
  (:require [clojure.java.io :as io])
  (:import [java.io File PushbackReader]))

(defn- read-ns-form [file]
  (binding [*read-eval*              false
            *default-data-reader-fn* tagged-literal]
    (with-open [reader (PushbackReader. (io/reader (io/file file)))]
      (read {:read-cond :preserve} reader))))

(defn- no-check [file]
  (:no-check (meta (second (read-ns-form file)))))

(defn- check-ns [file]
  (when-not (no-check file)
    (binding [*out* *err*]
      (println "Compiling namespace" file))
    (try
      (binding [*warn-on-reflection* true]
        (load-file file)
        nil)
      (catch ExceptionInInitializerError e
        (doto e .printStackTrace)))))

(defn- get-failures [source-paths]
  (sequence
   (comp
    (mapcat (comp file-seq io/file))
    (map #(.getAbsolutePath ^File %))
    (filter #(re-matches #".*\.cljc?$" %))
    (keep check-ns))
   source-paths))

(defn check [source-paths]
  (let [failures (count (get-failures source-paths))]
    (shutdown-agents)
    (when-not (zero? failures)
      (System/exit failures))))

(defn -main [& source-paths]
  (check (or (seq source-paths) ["src"])))
