(ns portal.runtime.jvm.editor-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [are deftest]]
            [portal.runtime]
            [portal.runtime.fs :as fs]
            [portal.runtime.jvm.editor :as editor]))

(deftest can-goto-test
  (are [value]
       (fs/exists (:file (editor/can-goto value)))
    ;; maps
    {:file "deps.edn"}
    {:ns 'portal.runtime}

    ;; vars
    #'portal.runtime/ops

    ;; namespace symbols
    'portal.runtime
    'portal.runtime/ops

      ;; urls
    (io/resource "portal/runtime.cljc")

    ;; files
    (io/file "deps.edn")

    ;; strings
    "deps.edn"
    "src/portal/runtime.cljc"
    (str "file:" (.getPath (.getAbsoluteFile (io/file "deps.edn"))))

    ;; string on classpath
    "portal/runtime.cljc")

  (are [value]
       (not (fs/exists (:file (editor/can-goto value))))
    ;; maps
    {:file "missing.edn"}
    {:ns 'ns.missing}
    {}

    ;; namespace symbols
    'ns.missing
    'ns.missing/conj

      ;; urls
    (io/resource "portal/missing.cljc")

    ;; files
    (io/file "missing.edn")

    ;; strings
    "missing.edn"
    "src/portal/missing.cljc"

    ;; string on classpath
    "portal/missing.cljc"))
