(ns ^:no-doc portal.runtime.fs
  (:refer-clojure :exclude [slurp spit list file-seq])
  #?(:clj  (:require [clojure.java.io :as io]
                     [clojure.string :as s])
     :cljs (:require ["fs" :as fs]
                     ["os" :as os]
                     ["path" :as path]
                     [clojure.string :as s])
     :cljr (:require [clojure.string :as s])
     :lpy  (:require [clojure.string :as s]))
  #?(:cljr (:import (System.IO Directory File Path))
     :lpy  (:import [pathlib :as p]
                    [os :as os]
                    [shutil :as shutil])))

(defn cwd []
  #?(:clj  (System/getProperty "user.dir")
     :cljs (.cwd js/process)
     :cljr (Directory/GetCurrentDirectory)
     :lpy  (os/getcwd)))

(defn slurp [path]
  #?(:clj  (clojure.core/slurp path)
     :cljs (fs/readFileSync path "utf8")
     :cljr (clojure.core/slurp path :enc "utf8")
     :lpy  (basilisp.core/slurp path :encoding "utf8")))

(defn spit [path content]
  #?(:clj  (clojure.core/spit path content)
     :cljs (fs/writeFileSync path content)
     :cljr (clojure.core/spit path content)
     :lpy  (basilisp.core/spit path content)))

(defn mkdir [path]
  #?(:clj  (.mkdirs (io/file path))
     :cljs (fs/mkdirSync path (clj->js {:recursive true}))
     :cljr (Directory/CreateDirectory path)
     :lpy  (os/makedirs path)))

(defn path []
  #?(:clj  (System/getenv "PATH")
     :cljs (.-PATH js/process.env)
     :cljr (Environment/GetEnvironmentVariable "PATH")
     :lpy  (.get os/environ "PATH")))

(defn separator []
  (or #?(:clj  (System/getProperty "path.separator")
         :cljs path/delimiter
         :cljr (str Path/PathSeparator))
      ":"))

(defn join [& paths]
  #?(:clj  (.getCanonicalPath ^java.io.File (apply io/file paths))
     :cljs (apply path/join paths)
     :cljr (Path/Join (into-array String paths))
     :lpy  (apply os.path/join paths)))

(defn exists [f]
  (when (and (string? f)
             #?(:clj  (.exists (io/file f))
                :cljs (fs/existsSync f)
                :cljr (or (File/Exists f)
                          (Directory/Exists f))
                :lpy  (.is_file (p/Path f))))
    f))

(defn is-file [f]
  (when (and f
             #?(:clj  (.isFile (io/file f))
                :cljs (and (exists f)
                           (.isFile (fs/lstatSync f)))
                :cljr (File/Exists f)
                :lpy  (.is_file (p/Path f))))
    f))

(defn modified [f]
  (when f
    #?(:clj  (.lastModified (io/file f))
       :cljs (.getTime ^js/Date (.-mtime (fs/lstatSync f)))
       :cljr (.ToUnixTimeMilliseconds
              (DateTimeOffset. (File/GetLastWriteTime f)))
       :lpy  (os.path/getmtime f))))

(defn can-execute [f]
  #?(:clj  (let [file (io/file f)]
             (and (.exists file) (.canExecute file) f))
     :cljs (when (not
                  (try (fs/accessSync f fs/constants.X_OK)
                       (catch :default _e true)))
             f)
     :cljr (exists f)
     :lpy  (os/access f os/X_OK)))

(defn paths []
  (s/split (or (path) "") (re-pattern (separator))))

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
     :cljr (Environment/GetFolderPath System.Environment+SpecialFolder/UserProfile)
     :lpy  (p.Path/home)))

(defn list [path]
  #?(:clj  (for [^java.io.File f (.listFiles (io/file path))]
             (.getAbsolutePath f))
     :cljs (for [f (fs/readdirSync path)]
             (join path f))
     :cljr (Directory/GetFileSystemEntries path)
     :lpy  (for [child (os/listdir path)]
             (join path child))))

(defn rm [path]
  #?(:clj  (let [children (list path)]
             (doseq [child children] (rm child))
             (io/delete-file path))
     :cljs (fs/rmSync path (clj->js {:recursive true}))
     :cljr (Directory/Delete path true)
     :lpy  (shutil/rmtree path)))

(defn rm-exit [path]
  #?(:clj  (.deleteOnExit (io/file path))
     :cljs (let [delete #(rm path)]
             (.on js/process "exit"    delete)
             (.on js/process "SIGINT"  delete)
             (.on js/process "SIGTERM" delete))))

(defn dirname [path]
  #?(:clj  (.getParent (io/file path))
     :cljs (let [root (.-root (path/parse path))]
             (when-not (= path root) (path/dirname path)))
     :cljr (some-> (Directory/GetParent path) str)
     :lpy  (let [root (os.path/dirname path)]
             (when-not (= path root) root))))

(defn basename [path]
  #?(:clj  (.getName (io/file path))
     :cljs (path/basename path)
     :cljr (Path/GetFileName path)
     :lpy  (os.path/basename path)))

(defn file-seq [dir]
  (tree-seq
   (fn [f] (not (is-file f)))
   (fn [d] (seq (list d)))
   (join dir)))