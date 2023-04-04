(ns tasks.cljr
  (:require [clojure.string :as str]
            [tasks.tools :refer [*opts* cljr]]))

(defn repl []
  (binding [*opts* {:inherit true
                    :extra-env {"CLOJURE_LOAD_PATH"
                                (str/join
                                 (System/getProperty "path.separator")
                                 ["src" "resources" "dev" "test"])}}]
    (cljr "--eval" "((requiring-resolve 'tasks.prepl/prepl))" "--repl")))

(defn -main [] (repl))
