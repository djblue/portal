(ns portal.runtime.jvm.editor
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [portal.runtime :as rt]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.launcher :as launcher]
            [portal.runtime.shell :refer [sh]])
  (:import (java.io File)
           (java.net URL URI)))

(defprotocol IResolve (resolve [this]))

(defn- exists [path]
  (when (.exists (io/file path)) {:file path}))

#?(:bb (def clojure.lang.Var (type #'exists)))
#?(:bb (def clojure.lang.Namespace (type *ns*)))

(extend-protocol IResolve
  clojure.lang.PersistentHashMap
  (resolve [m]
    (when-let [file (:file m)]
      (when-let [resolved (resolve file)]
        (merge m resolved))))
  clojure.lang.PersistentArrayMap
  (resolve [m]
    (when-let [file (:file m)]
      (when-let [resolved (resolve file)]
        (merge m resolved))))
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
       [".cljc" ".clj"])))
  clojure.lang.Symbol
  (resolve [^Symbol s]
    (resolve (find-ns s)))
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
  (some fs/can-execute
        ["code"
         "/Applications/Visual Studio Code.app/Contents/Resources/app/bin/code"]))

(defmethod -open-editor :vs-code [{:keys [line column file]}]
  (sh (get-vs-code) "--goto" (str file ":" line ":" column)))

(defmethod -open-editor :intellij [info]
  (when-let [{:keys [host port]} (launcher/get-config "intellij.edn")]
    (http/post
     (str "http://" host ":" port "/open-file")
     {:headers {"content-type" "application/edn"}
      :body (pr-str info)})))

(defn can-goto [input]
  (and (satisfies? IResolve input) (resolve input)))

(defn goto-definition
  "Goto the definition of a value in an editor."
  {:predicate can-goto :command true}
  [input]
  (when-let [location (can-goto input)]
    (-open-editor
     (assoc location
            :editor
            (get-in rt/*session* [:options :launcher] :emacs)))))
