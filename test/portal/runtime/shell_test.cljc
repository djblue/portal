(ns portal.runtime.shell-test
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime.shell :as sh]))

(deftest echo
  (is (= {:exit 0, :out ":hi\n", :err ""}
         (sh/sh "bb" "-e" ":hi"))))