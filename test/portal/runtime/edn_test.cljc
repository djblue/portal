(ns portal.runtime.edn-test
  (:require [clojure.test :refer [deftest is]]
            [portal.runtime.edn :as edn]))

(deftest read-string-test
  (let [tagged (edn/read-string "^{:my :meta} #'hi")]
    (is (= 'hi (:rep tagged)))
    (is (= "portal/var" (:tag tagged)))
    (is (= {:my :meta} (meta tagged))))
  (let [tagged (edn/read-string "#\"hi\"")]
    (is (= "portal/re" (:tag tagged)))
    (is (= "hi" (:rep tagged))))
  (let [tagged (edn/read-string (pr-str #"\\Qhi\\E"))]
    (is (= "portal/re" (:tag tagged)))
    (is (= "\\Qhi\\E" (:rep tagged)))))
