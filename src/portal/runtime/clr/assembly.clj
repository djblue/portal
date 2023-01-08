(ns ^:no-check portal.runtime.clr.assembly
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
     (assoc out package (find-dll (name package) (:nuget/version info))))
   {}
   deps))

(defn load-deps [deps]
  (doseq [[_package dll] (resolve-dlls deps)]
    #_{:clj-kondo/ignore [:unresolved-symbol]}
    (assembly-load-file dll)))

(def ^:private deps
  '{clojure.data.json {:nuget/version "2.4.0"}})

#_{:clj-kondo/ignore [:unresolved-symbol]}
(assembly-load "System.Text.Json")
(load-deps deps)
