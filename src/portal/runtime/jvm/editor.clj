(ns ^:no-doc portal.runtime.jvm.editor
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [portal.runtime :as rt]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.launcher :as launcher]
            [portal.runtime.shell :refer [spawn]])
  (:import (java.io File)
           (java.net URL URI)))

(defprotocol IResolve (resolve [this]))

(defn- find-file [file-name]
  (some
   (fn [^File file]
     (when (and (.isFile file)
                (= (.getName file) file-name))
       file))
   (concat
    (file-seq (io/file "src"))
    (file-seq (io/file "test")))))

(defn- exists [path]
  (when-let [file (or (fs/exists path) (find-file path))]
    {:file (.getAbsolutePath (io/file file))}))

(def clojure.lang.Var (type #'exists))
(def clojure.lang.Namespace (type *ns*))

(def ^:private mapping
  {:clojure.error/column :column
   :clojure.error/line   :line
   :clojure.error/source :file})

(defn- ns->paths [ns]
  (for [ext [".cljc" ".clj" ".cljs"]]
    (str (str/replace (munge ns) #"\." "/") ext)))

(defn- get-resource-path [^URL resource]
  (case (.getProtocol resource)
    "file" (let [file (.getFile resource)]
             (when (fs/exists file)
               {:file file}))
    "jar"  (let [[jar file] (str/split (.getFile resource) #"!/")
                 file (fs/join (fs/cwd) ".portal/jar" (fs/basename jar) file)]
             (when-not (fs/exists file)
               (fs/mkdir (fs/dirname file))
               (spit file (slurp resource)))
             {:file file})
    nil))

(defn- find-var* [s]
  (try (find-var s) (catch Exception _)))

(defn- resolve-map [m]
  (let [m (set/rename-keys m mapping)]
    (if (fs/is-file (:file m))
      (some->> m :file resolve (merge m))
      (or
       (some->> m :ns   resolve (merge m))
       (some->> m :file resolve (merge m))))))

(extend-protocol IResolve
  nil
  (resolve [_m] nil)

  Object
  (resolve [_m] nil)

  clojure.lang.PersistentHashMap
  (resolve [m] (resolve-map m))
  clojure.lang.PersistentArrayMap
  (resolve [m] (resolve-map m))

  clojure.lang.Var
  (resolve [v]
    (let [m (meta v)]
      (some->> m :file resolve (merge m))))
  clojure.lang.Namespace
  (resolve [ns]
    (resolve (symbol (str ns))))
  clojure.lang.Symbol
  (resolve [^clojure.lang.Symbol s]
    (or
     (when (namespace s) (some-> s find-var* resolve))
     (some->> s ns->paths (some io/resource) resolve)))
  URL
  (resolve [^URL url]
    (or (exists (.getPath url))
        (get-resource-path url)))
  URI
  (resolve [^URI url]
    (exists (.getPath url)))
  File
  (resolve [^File file]
    (exists (.getAbsolutePath file)))
  String
  (resolve [file]
    (or (exists file)
        (some-> file io/resource resolve))))

(defmulti -open-editor :editor)

(defmethod -open-editor :emacs [{:keys [line column file]}]
  (if-not line
    (spawn "emacsclient" file)
    (spawn "emacsclient" "-n" (str "+" line ":" column) file)))

(defn- get-vs-code []
  (or (fs/can-execute "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code")
      "code"))

(defn- extension-open-editor [info config]
  (let [file-info (select-keys info [:file :line :column])
        {:keys [error status] :as response}
        @(http/request
          {:url     (str "http://" (:host config) ":" (:port config) "/open-file")
           :method  :post
           :headers {"content-type" "application/edn"}
           :body    (pr-str file-info)})]
    (when (or error (not= status 200))
      (throw
       (ex-info "Unable to open file in intellij editor"
                {:file-info file-info
                 :config    config
                 :response  (select-keys response [:body :headers :status])}
                error)))))

(defmethod -open-editor :vs-code [{:keys [file line column] :as info}]
  (try
    (extension-open-editor info (launcher/get-config {:config-file "vs-code.edn"}))
    (catch Exception _
      (spawn (get-vs-code) "--goto" (str file ":" line ":" column)))))

(defmethod -open-editor :intellij [info]
  (extension-open-editor info (launcher/get-config {:config-file "intellij.edn"})))

(defmethod -open-editor :auto [info]
  (-open-editor
   (assoc info :editor
          (cond
            (fs/exists ".portal/vs-code.edn")  :vs-code
            (fs/exists ".portal/intellij.edn") :intellij
            :else                              :emacs))))

(defn can-goto [input]
  (or (and (satisfies? IResolve input) (resolve input))
      (when-let [m (meta input)] (recur m))))

(defn goto-definition
  "Goto the definition of a value in an editor."
  {:command true
   :predicate can-goto
   :shortcuts [["g" "d"]]}
  [input]
  (when-let [location (can-goto input)]
    (let [{:keys [options]} rt/*session*]
      (-open-editor
       (assoc location
              :editor
              (or (:editor options)
                  (:launcher options)
                  :auto)))
      true)))