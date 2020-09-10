(ns portal.jvm-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [portal.api :as p]
            [portal.main :as m]
            [portal.runtime.client.bb :as bb]
            [portal.server :as s]))

(defn- headless-chrome-flags [url]
  ["--headless" "--disable-gpu" url])

(defn- open [f]
  (with-redefs [m/chrome-flags f] (p/open)))

(defn- patch-html
  "Patch index.html with a wait.js script to ensure
  headless chrome doesn't exit early."
  []
  (spit "target/test.html"
        (str (slurp "resources/index.html")
             "<script src=\"wait.js\"></script>"))
  (alter-var-root #'s/resource assoc "index.html" (io/file "target/test.html")))

(deftest e2e-jvm
  (patch-html)
  (when-let [portal (open headless-chrome-flags)]
    (with-redefs [bb/timeout 10000]
      (reset! portal 0)
      (is (= @portal 0))
      (swap! portal inc)
      (is (= @portal 1))))
  (Thread/sleep 100)
  (p/close)
  (Thread/sleep 100))

