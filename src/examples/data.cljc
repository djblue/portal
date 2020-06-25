(ns examples.data
  (:require #?(:clj [clojure.java.io :as io])
            [portal.colors :as c]
            [examples.hacker-news :as hn])
  #?(:clj (:import [java.io File]
                   [java.net URI URL]
                   [java.util UUID])))

(def platform-data
  #?(:clj {::class File
           ::file (io/file "deps.edn")
           ::directory  (io/file ".")
           ::uri (URI. "http://www.google.com")
           ::url (URL. "http://www.google.com")
           ::exception (try (/ 1 0) (catch Exception e e))
           ::io-exception (try (slurp "/hello") (catch Exception e e))
           ::user-exception (Exception. "hi")
           ::uuid (UUID/randomUUID)
           ::deps (read-string (slurp "deps.edn"))
           ::date (java.util.Date.)}
     :cljs {::promise (js/Promise.resolve 123)
            ::url (js/URL. "http://www.google.com")
            ::uuid (random-uuid)
            ::date (js/Date.)}))

(def data
  (merge
   platform-data
   {::hacker-news hn/stories
    ::themes c/themes
    ::regex #"hello-world"
    ::ns *ns*
    ::with-meta (with-meta [1 2 3] {:hello :world})
    ::booleans #{true false}
    ::nil nil
    ::vector [1 2 4]
    "string-key" "string-value"
    ::list (list 1 2 3)
    ::set #{1 2 3}
    {:example/settings 'complex-key} :hello-world
    #{1 2 3} [4 5 6]
    ::var #'portal.colors/themes
    ::ns-symbol 'hello/world
    ::keyword :hello-world
    ::ns-keyword ::hello-world
    ::range (range 10)
    ::nested-vector [1 2 3 [4 5 6]]
    ::code '(defn hello-world [] (println "hello, world"))}))
