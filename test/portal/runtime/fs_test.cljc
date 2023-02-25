(ns portal.runtime.fs-test
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime.fs :as fs]))

(deftest fs
  (is (some? (fs/slurp "deps.edn")))
  (let [deps (fs/join (fs/cwd) "deps.edn")]
    (is (= (fs/exists deps) deps)))
  (is (some? (fs/home)))
  (is (some? (seq (fs/paths))))
  (is (some? (fs/is-file "deps.edn")))
  (is (nil? (fs/is-file "deps.end")))
  (is (contains?
       (into #{} (fs/list (fs/cwd)))
       (fs/join (fs/cwd) "deps.edn")))
  (let [dir  (str "target/" (gensym))
        file (str dir "/" (gensym))]
    (fs/mkdir dir)
    (fs/spit file "hello")
    (is (= (fs/slurp file) "hello"))
    (fs/rm dir)
    (is (nil? (fs/exists file)))
    (is (nil? (fs/exists dir))))
  (let [cwd  (fs/cwd)
        path (fs/join cwd "deps.edn")]
    (is (= cwd (fs/dirname path))))
  (is (nil? (fs/dirname "/"))))
