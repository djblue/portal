(ns examples.data
  (:require [portal.colors :as c]
            [examples.hacker-news :as hn]))

(def platform-data
  #?(:clj {::class java.io.File
           ::file (clojure.java.io/file "deps.edn")
           ::directory  (clojure.java.io/file ".")
           ::uri (java.net.URI. "http://www.google.com")
           ::url (java.net.URL. "http://www.google.com")
           ::exception (try (/ 1 0) (catch Exception e e))
           ::io-exception (try (slurp "/hello") (catch Exception e e))
           ::user-exception (Exception. "hi")
           ::uuid (java.util.UUID/randomUUID)
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
