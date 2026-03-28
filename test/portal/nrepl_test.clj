(ns portal.nrepl-test
  (:require
   [clojure.test :refer [deftest is]]
   [portal.nrepl :as nrepl]))

(deftest parse-warn-on-reflection-test
  (is (=
       '[{:tag :err :val "hello\n"}
         {:tag :tap
          :val
          {:level :warn
           :ns user
           :file "dev/user.clj"
           :line 1
           :column 1
           :result "hello world"}}
         {:tag :err, :val "world\n"}]
       (-> (str "hello\n"
                "Reflection warning, dev/user.clj:1:1 - hello world.\n"
                "world\n")
           (#'nrepl/parse-warn-on-reflection)
           (vec)
           (update-in [1 :val] dissoc :time)))))