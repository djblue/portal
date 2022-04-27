(ns tasks.info
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(def version "0.25.0")

(defn git-hash []
  (str/trim (:out (sh "git" "rev-parse" "HEAD"))))

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
     (provided (get-in deps [:aliases :cljs :extra-deps]))
     (provided (get-in deps [:aliases :plk :extra-deps])))))

(def options
  {:lib           'djblue/portal
   :description   "A clojure tool to navigate through your data."
   :version       version
   :url           "https://github.com/djblue/portal"
   :src-dirs      ["src"]
   :resource-dirs ["resources"]
   :jar-file      (str "./target/portal-" version ".jar")
   :class-dir     "target/classes"
   :repos         {"clojars" {:url "https://repo.clojars.org/"}}
   :scm           {:tag (git-hash)
                   :url "https://github.com/djblue/portal"}
   :license
   {:name "MIT License"
    :url  "https://opensource.org/licenses/MIT"}
   :deps          (get-deps)})

(defn -main []
  (println (str "::set-output name=version::" version)))
