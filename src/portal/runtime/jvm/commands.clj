(ns ^:no-doc portal.runtime.jvm.commands
  (:require [portal.runtime :as rt]
            [portal.runtime.jvm.editor :as editor])
  (:import [java.io File]
           [java.net URI URL]))

(defn- can-slurp? [value]
  (or (string? value)
      (instance? URI value)
      (instance? URL value)
      (and (instance? File value)
           (.isFile ^File value)
           (.canRead ^File value))))

(rt/register! #'bean)
(rt/register! #'slurp {:predicate can-slurp?})
(rt/register! #'editor/goto-definition)

(def ^:private in-bb? (some? (System/getProperty "babashka.version")))

(when-not in-bb?
  (try
    (rt/register! (requiring-resolve `clojure.spec.alpha/exercise))
    (catch Exception _)))
