(ns tasks.e2e
  (:require [babashka.process :as p]
            [clojure.java.io :as io]
            [portal.e2e :as e2e]
            [tasks.build :refer [build]]))

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
   :bb   [:bb]
   :clr  [:bb "-m" "tasks.cljr/repl"]})

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

(defn all
  "Run e2e tests in all envs."
  []
  (dorun (map e2e (keys e2e-envs))))

(defn -main [] (all))
