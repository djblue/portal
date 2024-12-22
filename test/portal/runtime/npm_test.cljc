(ns portal.runtime.npm-test
  (:require [clojure.test :refer [are deftest]]
            [portal.runtime.npm :refer [node-resolve]]))

(deftest valid-modules
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
