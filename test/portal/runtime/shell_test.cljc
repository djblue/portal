(ns portal.runtime.shell-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [portal.runtime.shell :as sh]))

(deftest echo
  (is (= ":hi" (some-> (sh/sh "bb" "-e" ":hi") :out str/trim))))