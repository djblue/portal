(ns ^:no-doc portal.runtime.fs
  (:refer-clojure :exclude [slurp spit list])
  #?(:clj  (:require [clojure.java.io :as io]
                     [clojure.string :as s])
     :cljs (:require ["fs" :as fs]
                     ["os" :as os]
                     ["path" :as path]
                     [clojure.string :as s])
     :cljr (:require [clojure.string :as s]))
  #?(:cljr (:import (System.IO Directory File Path))))

(defn cwd []
  #?(:clj  (System/getProperty "user.dir")
     :cljs (.cwd js/process)
     :cljr (Directory/GetCurrentDirectory)))

(defn slurp [path]
  #?(:clj  (clojure.core/slurp path)
     :cljs (fs/readFileSync path "utf8")
     :cljr (clojure.core/slurp path :enc "utf8")))

(defn spit [path content]
  #?(:clj  (clojure.core/spit path content)
     :cljs (fs/writeFileSync path content)
     :cljr (clojure.core/spit path content)))

(defn mkdir [path]
  #?(:clj  (.mkdirs (io/file path))
     :cljs (fs/mkdirSync path #js {:recursive true})
     :cljr (Directory/CreateDirectory path)))

(defn path []
  #?(:clj  (System/getenv "PATH")
     :cljs (.-PATH js/process.env)
     :cljr (Environment/GetEnvironmentVariable "PATH")))

(defn separator []
  (or #?(:clj  (System/getProperty "path.separator")
         :cljs path/delimiter
         :cljr (str Path/PathSeparator))
      ":"))

(defn join [& paths]
  #?(:clj  (.getAbsolutePath ^java.io.File (apply io/file paths))
     :cljs (apply path/join paths)
     :cljr (Path/Join (into-array String paths))))

(defn exists [f]
  (when #?(:clj  (.exists (io/file f))
           :cljs (fs/existsSync f)
           :cljr (File/Exists f))
    f))

(defn can-execute [f]
  #?(:clj  (let [file (io/file f)]
             (and (.exists file) (.canExecute file) f))
     :cljs (when (not
                  (try (fs/accessSync f fs/constants.X_OK)
                       (catch js/Error _e true)))
             f)
     :cljr (exists f)))

(defn paths []
  (s/split (path) (re-pattern (separator))))

(defn find-bin [paths files]
  (first
   (for [file files
         path paths
         :let [f (join path file)]
         :when (can-execute f)]
     f)))

(defn home []
  #?(:clj  (System/getProperty "user.home")
     :cljs (os/homedir)
     :cljr (or (Environment/GetEnvironmentVariable "HOME")
               (Environment/GetEnvironmentVariable "userdir"))))

(defn list [path]
  #?(:clj  (for [^java.io.File f (.listFiles (io/file path))]
             (.getAbsolutePath f))
     :cljs (for [f (fs/readdirSync path)]
             (join path f))
     :cljr (Directory/GetFiles path)))

(defn rm [path]
  #?(:clj  (let [children (list path)]
             (doseq [child children] (rm child))
             (io/delete-file path))
     :cljs (fs/rmSync path #js {:recursive true})
     :cljr (Directory/Delete path true)))

(defn rm-exit [path]
  #?(:clj  (.deleteOnExit (io/file path))
     :cljs (let [delete #(rm path)]
             (.on js/process "exit"    delete)
             (.on js/process "SIGINT"  delete)
             (.on js/process "SIGTERM" delete))))

(defn dirname [path]
  #?(:clj  (.getParent (io/file path))
     :cljs (path/dirname path)
     :cljr (str (Directory/GetParent path))))
