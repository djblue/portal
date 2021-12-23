(ns tasks.info
  (:require [babashka.process :as p]
            [clojure.string :as str]))

(def version "0.19.0")

(defn git-hash []
  (-> ["git" "rev-parse" "HEAD"]
      (p/process {:out :string})
      p/check
      :out
      str/trim))

(defn- provided [deps]
  (reduce-kv
   (fn [m k v]
     (assoc m k (assoc v :scope "provided")))
   {}
   deps))

(defn- get-deps []
  (let [deps (read-string (slurp "deps.edn"))]
    (merge
     (:deps deps)
     (provided (get-in deps [:aliases :cider :extra-deps]))
     (provided (get-in deps [:aliases :cljs :extra-deps])))))

(def options
  {:lib 'djblue/portal
   :description "A clojure tool to navigate through your data."
   :version version
   :url "https://github.com/djblue/portal"
   :src-dirs ["src"]
   :resource-dirs [""]
   :resources
   {"src" {:excludes ["portal/extensions/**"
                      "examples/**"]}
    "resources/portal/" {:target "portal/"}}
   :repos {"clojars" {:url "https://repo.clojars.org/"}}
   :scm {:tag (git-hash)}
   :license
   {:name "MIT License"
    :url  "https://opensource.org/licenses/MIT"}
   :deps (get-deps)})

(defn -main [] (println options))
