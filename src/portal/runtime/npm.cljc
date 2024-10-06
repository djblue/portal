(ns ^:no-doc portal.runtime.npm
  (:require #?(:clj [clojure.java.io :as io])
            [clojure.string :as str]
            [portal.runtime.fs :as fs]
            [portal.runtime.json :as json]))

(defn- package-resolve [module]
  (when-let [package (fs/exists (fs/join module "package.json"))]
    (fs/exists
     (fs/join
      module
      (let [json    (json/read (fs/slurp package) {})
            umd     (get-in json ["exports" "umd"])
            unpkg   (get json "unpkg")
            browser (get json "browser")
            main    (get json "main")]
        (or umd
            (when (string? browser)
              browser)
            (get browser main)
            (get browser (str "./" main))
            main
            unpkg
            "index.js"))))))

#_{:clj-kondo/ignore #?(:clj [] :default [:unused-binding])}
(defn- resource-resolve [file]
  #?(:clj (some->
           (io/file
            (or (io/resource file)
                (io/resource (str file ".js"))))
           str)))

(defn- relative-resolve [module root]
  (let [path (fs/join root module)]
    (or (fs/is-file path)
        (fs/is-file (str path ".js"))
        (fs/is-file (fs/join path "index.js")))))

(defn- relative? [module] (str/starts-with? module "."))

(defn- get-parents [root]
  (->> root
       (iterate fs/dirname)
       (take-while some?)
       (map #(fs/join % "node_modules"))))

(defn node-resolve
  ([module]
   (if (relative? module)
     (or (relative-resolve module (fs/cwd))
         (resource-resolve module))
     (node-resolve module (fs/cwd))))
  ([module root]
   (if (relative? module)
     (relative-resolve module root)
     (let [search-paths (get-parents root)]
       (or
        (some
         (fn [root]
           (let [path (fs/join root module)]
             (or
              (fs/is-file path)
              (fs/is-file (str path ".js"))
              (package-resolve path)
              (fs/is-file (fs/join path "index.js")))))
         search-paths)
        (throw
         (ex-info (str "Unable to find node module: " (pr-str module))
                  {:module module :search-paths search-paths})))))))
