(ns tasks
  (:refer-clojure :exclude [test])
  (:require [babashka.deps :as deps]
            [babashka.fs :as fs]
            [babashka.process :as p]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [portal.e2e :as e2e]
            [pwa]
            [version]))

(def version "0.10.0")

(defn- sh [& args]
  (println "=>" (str/join " " (map name args)))
  (let [opts {:inherit true}
        ps (if-not (= (first args) :clojure)
             (p/process (map name args) opts)
             (deps/clojure (map name (rest args)) opts))]
    (p/check ps))
  nil)

(def bb     (partial sh :bb))
(def clj    (partial sh :clojure))
(def git    (partial sh :git))
(def mvn    (partial sh :mvn))
(def node   (partial sh :node))
(def npm    (partial sh :npm))
(def npx    (partial sh :npx))
(def shadow (partial clj "-M:cljs:shadow"))

(defn install []
  (when-not (fs/exists? "node_modules")
    (npm :ci)))

(defn check []
  (clj "-M:cider:check")
  (clj "-M:kondo" "--lint" :dev :src :test)
  (clj "-M:cljfmt" :check))

(defn fmt [] (clj "-M:cljfmt" :fix))

(defn check-deps []
  (npm :outdated)
  (clj "-M:antq" "-m" :antq.core))

(defn fix-deps []
  (npm :update)
  (clj "-M:antq" "-m" :antq.core "--upgrade"))

(defn main-js []
  (install)
  (when-not (fs/exists? "resources/portal/main.js")
    (shadow :release :client)))

(defn ws-js []
  (install)
  (let [out "resources/portal/ws.js"]
    (when-not (fs/exists? out)
      (npx :browserify
           "--node"
           "--exclude" :bufferutil
           "--exclude" :utf-8-validate
           "--standalone" :Server
           "--outfile" out
           "node_modules/ws"))))

(defn build [] (main-js) (ws-js))

(defn test-cljs [version]
  (let [out (str "target/test." version ".js")]
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
         "--compile" :portal.test-runner)
    (node out)))

(defn test []
  (test-cljs "1.10.773")
  (test-cljs "1.10.844")
  (build)
  (clj "-M:test" "-m" :portal.test-runner)
  (bb "-m" :portal.test-runner))

(defn rm [path]
  (when (fs/exists? path)
    (println "=>" "rm" path)
    (fs/delete-tree path))
  nil)

(defn clean []
  (rm "resources/portal/")
  (rm "target/"))

(defn ci [] (clean) (check) (test))

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

(defn e2e-all [] (dorun (map e2e (keys e2e-envs))))

(defn app []
  (fs/create-dirs "target/pwa-release/")
  (pwa/-main :prod)
  (shadow :release :pwa))

(defn set-version []
  (version/-main version)
  (git :add ".")
  (git :commit "-m" (str "Release " version)))

(defn pom [] (clj "-Spom"))

(defn jar []
  (build)
  (when-not (fs/exists? (str "target/portal-" version ".jar"))
    (mvn :package)))

(defn release [] (clean) (ci) (jar))

(defn deploy [] (release) (mvn :deploy))
