(ns notebook.ci
  (:require [tasks.build :refer [build install]]
            [tasks.check :as check]
            [tasks.format :as fmt]
            [tasks.parallel :refer [with-out-data]]
            [tasks.test :as test]
            [tasks.tools :as tool]))

(check/cloc)

(with-out-data (fmt/check))
(with-out-data (check/clj-kondo))
(with-out-data (check/clj-check))
(with-out-data (check/gradle-check))

(install)
(with-out-data (test/cljs* "1.10.773"))
(with-out-data (test/cljs* "1.10.844"))

(build)
(with-out-data (tool/clj "-M:test" "-m" :portal.test-runner))
(with-out-data (tool/bb "-m" :portal.test-runner))
(with-out-data (test/cljr))
