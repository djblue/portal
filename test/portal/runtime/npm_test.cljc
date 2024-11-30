(ns portal.runtime.npm-test
  (:require [clojure.test :refer [deftest are]]
            [portal.runtime.shell :refer [sh]]
            [portal.runtime.npm :refer [node-resolve]]))

(deftest valid-modules
  (sh "npm" "install" "react@^17.0.2")
  (are [module]
       (some? (node-resolve module))
    "react/jsx-runtime.js"
    "react/index"
    "react"))

(deftest invalid-modules
  (are [module]
       (thrown?
        #?(:clj Exception :cljr Exception :cljs js/Error)
        (node-resolve module))
    "react/index.j"))
