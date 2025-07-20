(ns ^:no-doc ^:no-check portal.runtime.clr.assembly
  (:require [portal.runtime.fs :as fs]))

(defn- find-dll [package version]
  (fs/exists
   (fs/join (fs/home) ".nuget/packages"
            package
            version
            "lib/netstandard2.1"
            (str package ".dll"))))

(defn- resolve-dlls [deps]
  (reduce-kv
   (fn [out package info]
     (let [dll (find-dll (name package) (:nuget/version info))]
       (cond-> out dll (assoc package dll))))
   {}
   deps))

(defn load-deps [deps]
  (doseq [[_package dll] (resolve-dlls deps)]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (assembly-load-file dll)))

#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Diagnostics.Process")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Text.Encoding")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Text.Json")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Net.WebSockets")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Net")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Net.HttpListener")
#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Net.Http")

(def ^:private deps
  '{clojure.data.json {:nuget/version "2.4.0"}})

(load-deps deps)

; (assembly-load-from (str clojure.lang.RT/SystemRuntimeDirectory "System.Diagnostics.Process.dll"))