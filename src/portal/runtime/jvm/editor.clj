(ns portal.runtime.jvm.editor
  (:refer-clojure :exclude [resolve])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [portal.runtime.shell :refer [sh]])
  (:import (clojure.lang Namespace Symbol Var PersistentArrayMap)
           (java.io File)
           (java.net URL URI)))

(defprotocol IResolve (resolve [this]))

(defn- exists [path]
  (when (.exists (io/file path)) {:file path}))

(extend-protocol IResolve
  PersistentArrayMap
  (resolve [m]
    (when-let [file (:file m)]
      (when-let [resolved (resolve file)]
        (merge m resolved))))
  Var
  (resolve [^Var v]
    (let [m (meta v)]
      (merge m (resolve (:file m)))))
  Namespace
  (resolve [^Namespace ns]
    (let [base (str/escape (str ns) {\. "/" \- "_"})]
      (some
       (fn [ext]
         (when-let [url (io/resource (str base ext))]
           (resolve url)))
       [".cljc" ".clj"])))
  Symbol
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
        (resolve (io/resource file)))))

(defmulti -open-editor :editor)

(defmethod -open-editor :emacs [{:keys [line column file]}]
  (if-not line
    (sh "emacsclient" file)
    (sh "emacsclient" "-n" (str "+" line ":" column) file)))

(defmethod -open-editor :vs-code [{:keys [line column file]}]
  (sh "code" "--goto" (str file ":" line ":" column)))

(defn can-open [input]
  (and (satisfies? IResolve input) (resolve input)))

(defn ^{:predicate can-open} open
  "Open value in editor."
  [input]
  (when-let [location (can-open input)]
    (-open-editor (assoc location :editor :emacs))))

(comment
  ;; unsupported
  (open :foo)
  (open 'clojure.core)

  (open *ns*)
  (open "/tmp")
  (open #'open)

  ;; user
  (open 'user)
  (open (io/resource "user.clj"))
  (open (io/file "dev/user.clj"))
  (open "user.clj")
  (open "dev/user.clj")
  (open (find-ns 'user)))
