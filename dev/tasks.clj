(ns tasks
  "All portal project tasks."
  (:refer-clojure :exclude [test format])
  (:require [babashka.deps :as deps]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [io.aviso.ansi :as a]
            [pom]
            [portal.e2e :as e2e]
            [pwa]
            [version]))

(def version "0.18.2")

(def ^:dynamic *cwd* nil)

(defn- sh [& args]
  (println (a/bold-blue "=>")
           (a/bold-green (name (first args)))
           (a/bold (str/join " " (map name (rest args)))))
  (let [opts {:inherit true :dir *cwd*}
        ps (if-not (= (first args) :clojure)
             (p/process (map name args) opts)
             (deps/clojure (map name (rest args)) opts))]
    (p/check ps))
  nil)

(def bb     (partial sh :bb))
(def clj    (partial sh :clojure))
(def git    (partial sh :git))
(def gradle (partial sh "./gradlew" "--warning-mode" "all"))
(def mvn    (partial sh :mvn))
(def node   (partial sh :node))
(def npm    (partial sh :npm))
(def npx    (partial sh :npx))
(def shadow (partial clj "-M:cljs:shadow"))

(defn assert-clean []
  (git :diff "--exit-code"))

(defn git-hash []
  (-> ["git" "rev-parse" "HEAD"]
      (p/process {:out :string})
      p/check
      :out
      str/trim))

(defn install []
  (when (seq
         (fs/modified-since
          "node_modules"
          ["package.json" "package-lock.json"]))
    (npm :ci)))

(defn check
  "Run all static analysis checks."
  []
  (clj "-M:cljfmt" :check
       :dev :src :test
       "extension-intellij/src/main/clojure")
  (clj "-M:kondo"
       "--lint" :dev :src :test
       "extension-intellij/src/main/clojure")
  (clj "-M:cider:check")
  (binding [*cwd* "extension-intellij"]
    (gradle "checkClojure")))

(defn format
  "Run cljfmt formatter."
  []
  (clj "-M:cljfmt" :fix
       :dev :src :test
       "extension-intellij/src/main/clojure"))

(defn check-deps []
  (npm :outdated)
  (clj "-M:antq" "-m" :antq.core))

(defn fix-deps
  "Update npm and clj dependencies."
  []
  (assert-clean)
  (npm :update)
  (clj "-M:antq" "-m" :antq.core "--upgrade")
  (binding [*cwd* "extension-vscode"]
    (npm :update))
  (git :add "-u")
  (git :commit "-m" "Bump deps"))

(defn main-js []
  (install)
  (when (seq
         (fs/modified-since
          "resources/portal/main.js"
          (concat
           ["deps.edn"
            "package.json"
            "package-lock.json"
            "shadow-cljs.edn"]
           (fs/glob "src/portal/ui" "**.cljs"))))
    (shadow :release :client)))

(defn ws-js []
  (install)
  (let [out "resources/portal/ws.js"]
    (when (seq
           (fs/modified-since
            out
            ["package.json" "package-lock.json"]))
      (npx :browserify
           "--node"
           "--exclude" :bufferutil
           "--exclude" :utf-8-validate
           "--standalone" :Server
           "--outfile" out
           "node_modules/ws"))))

(defn build [] (main-js) (ws-js))

(defn intellij-extension
  "Build intellij extension."
  []
  (binding [*cwd* "extension-intellij"]
    (gradle "buildPlugin")
    (println (str "extension-intellij/build/distributions/portal-extension-intellij-" version ".zip"))))

(defn vs-code-extension
  "Build vs-code extension."
  []
  (build)
  (shadow :release :vs-code)
  (binding [*cwd* "extension-vscode"]
    (npm :ci)
    (fs/copy "README.md" "extension-vscode/" {:replace-existing true})
    (fs/copy "LICENSE" "extension-vscode/LICENSE" {:replace-existing true})
    (npx :vsce :package)))

(defn build-extensions
  "Build editor extensions."
  []
  (vs-code-extension)
  (intellij-extension))

(defn dev
  "Start dev server."
  []
  (build)
  (clj "-M:dev:cider:cljs:shadow" :watch :pwa :client :vs-code))

(defn setup-planck []
  (sh :sudo :add-apt-repository "ppa:mfikes/planck")
  (sh :sudo :apt-get :update)
  (sh :sudo :apt-get :install :planck))

(defn test-cljs [version]
  (let [out (str "target/test." version ".js")]
    (when (seq
           (fs/modified-since
            out
            (concat
             (fs/glob "src" "**")
             (fs/glob "test" "**"))))
      (clj "-Sdeps"
           (pr-str
            {:deps
             {'org.clojure/clojurescript
              {:mvn/version version}}})
           "-M:test"
           "-m" :cljs.main
           "--output-dir" (str "target/cljs-output-" version)
           "--target" :node
           "--output-to" out
           "--compile" :portal.test-runner))
    (node out)))

(defn test
  "Run all clj/s tests."
  []
  (test-cljs "1.10.773")
  (test-cljs "1.10.844")
  (sh :planck "-c" "src:test" "-m" :portal.test-planck)
  (build)
  (clj "-M:test" "-m" :portal.test-runner)
  (bb "-m" :portal.test-runner))

(defn rm [path]
  (when (fs/exists? path)
    (println "=>" "rm" path)
    (fs/delete-tree path))
  nil)

(def ^:private clean-files
  ["extension-intellij/build/"
   "extension-vscode/vs-code.js"
   "extension-vscode/vs-code.js.map"
   "pom.xml"
   "resources/portal/"
   "target/"
   (str "extension-vscode/portal-" version ".vsix")])

(defn clean
  "Remove target and resources/portal"
  []
  (doseq [file clean-files] (rm file)))

(defn ci
  "Run all CI Checks."
  []
  (check) (test))

(def e2e-envs
  {:jvm  [:clojure "-M" "-e" "(set! *warn-on-reflection* true)" "-r"]
   :node [:clojure
          "-Sdeps"
          (pr-str
           {:deps
            {'org.clojure/clojurescript
             {:mvn/version "1.10.844"}}})
          "-M" "-m" :cljs.main "-re" :node]
   :web  [:clojure
          "-Sdeps"
          (pr-str
           {:deps
            {'org.clojure/clojurescript
             {:mvn/version "1.10.844"}}})
          "-M" "-m" :cljs.main]
   :bb   [:bb]})

(defn e2e [env]
  (build)
  (let [env (if (keyword? env) env (read-string env))
        ps  (p/process (map name (get e2e-envs env)) {:out :inherit :err :inherit})]
    (println "running e2e tests for" env)
    (when (= env :web)
      (println "please wait for browser to open before proceeding"))
    (binding [*out* (io/writer (:in ps))]
      (e2e/-main (name env)))
    (.close (:in ps))
    (p/check ps)
    nil))

(defn e2e-all
  "Run e2e tests in all envs."
  []
  (dorun (map e2e (keys e2e-envs))))

(defn app
  "Build portal PWA. (djblue.github.io/portal)"
  []
  (fs/create-dirs "target/pwa-release/")
  (pwa/-main :prod)
  (shadow :release :pwa))

(defn tag
  "Commit and tag a version."
  []
  (version/-main version)
  (git :add "-u")
  (git :commit "-m" (str "Release " version))
  (git :tag version))

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

(defn pom []
  (let [target "pom.xml"]
    (when (seq
           (fs/modified-since
            target
            ["deps.edn"]))
      (println "=>" "writing" target)
      (spit target (pom/generate options)))))

(defn jar
  "Build a jar."
  []
  (build)
  (pom)
  (when (seq
         (fs/modified-since
          (str "target/portal-" version ".jar")
          (concat
           ["pom.xml"]
           (fs/glob "src" "**")
           (fs/glob "resources" "**"))))
    (mvn :package)))

(defn package-artifacts
  "Package all release artifacts."
  []
  (jar)
  (build-extensions))

(defn- deploy-vscode []
  (binding [*cwd* "extension-vscode"]
    (npx :vsce :publish)))

(defn- deploy-clojars []
  (mvn :deploy))

(defn deploy
  "Deploy to clojars."
  []
  (ci)
  (package-artifacts)
  (deploy-clojars)
  (deploy-vscode))

(defn- get-task [task]
  (find-var
   (get-in (read-string (slurp "bb.edn")) [:tasks task])))

(defn -main [task & args]
  (apply (get-task (symbol task)) args))
