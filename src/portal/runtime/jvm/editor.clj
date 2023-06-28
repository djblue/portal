(ns ^:no-doc portal.runtime.jvm.editor
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [portal.runtime :as rt]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.launcher :as launcher]
            [portal.runtime.shell :refer [sh]])
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

(extend-protocol IResolve
  clojure.lang.PersistentHashMap
  (resolve [m]
    (let [m (set/rename-keys m mapping)]
      (when-let [file (or (:file m) (:ns m))]
        (when-let [resolved (resolve file)]
          (merge m resolved)))))
  clojure.lang.PersistentArrayMap
  (resolve [m]
    (let [m (set/rename-keys m mapping)]
      (when-let [file (or (:file m) (:ns m))]
        (when-let [resolved (resolve file)]
          (merge m resolved)))))
  clojure.lang.Var
  (resolve [v]
    (let [m (meta v)]
      (merge m (resolve (:file m)))))
  clojure.lang.Namespace
  (resolve [ns]
    (let [base (str/escape (str ns) {\. "/" \- "_"})]
      (some
       (fn [ext]
         (when-let [url (io/resource (str base ext))]
           (resolve url)))
       [".cljc" ".clj" ".cljs"])))
  clojure.lang.Symbol
  (resolve [^clojure.lang.Symbol s]
    (or
     (some-> s namespace symbol find-ns resolve (merge (meta s)))
     (some-> s find-ns resolve (merge (meta s)))))
  URL
  (resolve [^URL url]
    (exists (.getPath url)))
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
    (sh "emacsclient" file)
    (sh "emacsclient" "-n" (str "+" line ":" column) file)))

(defn- get-vs-code []
  (or (fs/can-execute "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code")
      "code"))

(defmethod -open-editor :vs-code [{:keys [line column file]}]
  (sh (get-vs-code) "--goto" (str file ":" line ":" column)))

(defmethod -open-editor :intellij [info]
  (let [file-info (select-keys info [:file :line :column])
        config    (launcher/get-config {:config-file "intellij.edn"})
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

(defn can-goto [input]
  (or (and (satisfies? IResolve input) (resolve input))
      (when-let [m (meta input)] (recur m))))

(defn goto-definition
  "Goto the definition of a value in an editor."
  {:predicate can-goto :command true}
  [input]
  (when-let [location (can-goto input)]
    (let [{:keys [options]} rt/*session*]
      (-open-editor
       (assoc location
              :editor
              (or (:editor options)
                  (:launcher options)
                  :emacs)))
      true)))
