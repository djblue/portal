(ns portal.runtime.npm-test
  (:require
   [clojure.test :refer [are deftest]]
   [portal.runtime.fs :as fs]
   [portal.runtime.npm :refer [node-resolve]]
   [portal.runtime.shell :refer [sh]]))

(deftest valid-modules
  (when-not (fs/exists "node_modules")
    (sh "npm" "install" "react@^17.0.2"))
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
