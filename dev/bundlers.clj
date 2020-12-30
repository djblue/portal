(ns bundlers
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [portal.api :as p]))

(defn sh [& args]
  (let [start  (. System (nanoTime))
        result (apply shell/sh (map name args))]
    (assoc result
           :time
           (/ (double (- (. System (nanoTime)) start)) 1000000.0))))

(defn versions [command]
  (str/split-lines (:out (sh :npx command "--version"))))

(defn size [in] (.length (io/file in)))

(defn gzip-size [in]
  (sh :gzip in)
  (size (str in ".gz")))

(defn shadow-cljs [_in]
  (let [out      "resources/portal/main.js"
        command  [:clojure "-M:cljs:shadow-cljs" :release :client]
        result   (apply sh command)]
    {:name     :shadow-cljs
     :command  command
     :versions ["2.11.10"]
     :size     (size out)
     :gzip     (gzip-size "resources/portal/main.js")
     :time     (:time result)}))

(defn browserify [in]
  (let [out      "./target/browserify.js"
        command  [:npx :browserify in
                  "-g" "[" :envify "--NODE_ENV" :production "]"
                  "-g" :uglifyify
                  "--outfile" out]
        result   (apply sh command)]
    {:name     :browserify
     :command  command
     :versions (versions :browserify)
     :size     (size out)
     :gzip     (gzip-size out)
     :time     (:time result)}))

(defn webpack [in]
  (let [dir      "./target/webpack/"
        out      (str dir "/main.js")
        command  [:npx :webpack "--entry" in "-o" dir]
        result   (apply sh command)]
    {:name     :webpack
     :command  command
     :versions (versions :webpack)
     :size     (size out)
     :gzip     (gzip-size out)
     :time     (:time result)}))

(defn esbuild [in]
  (let [out      "./target/esbuild.js"
        command  [:npx :esbuild in
                  "--bundle"
                  "--minify"
                  "--define:process.env.NODE_ENV=\"production\""
                  (str "--outfile=" out)]
        result   (apply sh command)]
    {:name     :esbuild
     :command  command
     :versions (versions :esbuild)
     :size     (size out)
     :gzip     (gzip-size out)
     :time     (:time result)}))

(defn rollup [in]
  (let [out      "./target/rollup.js"
        command  [:npx :rollup in
                  "--file" out
                  "--format" :iife]
        result   (apply sh command)]
    {:name     :rollup
     :command  command
     :versions (versions :rollup)
     :size     (size out)
     :gzip     (gzip-size out)
     :time     (:time result)}))

(defn parcel [in]
  (let [dir      "./target/parcel/"
        out      (str dir "/index.js")
        command  [:npx :parcel :build in
                  "--out-dir" dir]
        result   (apply sh command)]
    {:name     :parcel
     :command  command
     :versions (versions :parcel)
     :size     (size out)
     :gzip     (gzip-size out)
     :time     (:time result)}))

(defn -main []
  (p/open)
  (p/tap)
  (println "Press CTRL+C to exit")
  (pmap
   (fn [bundler]
     (tap> (bundler "./out/index.js")))
   [shadow-cljs browserify webpack esbuild parcel])
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. #(p/close)))
  @(promise))

