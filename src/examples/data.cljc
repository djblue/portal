(ns examples.data
  (:require #?(:clj [clojure.java.io :as io])
            [examples.macros :refer [read-file]]
            [portal.colors :as c]
            [examples.hacker-news :as hn])
  #?(:clj (:import [java.io File ByteArrayOutputStream]
                   [java.net URI URL]
                   [java.util UUID])))

#?(:clj
   (defn slurp-bytes [x]
     (with-open [out (ByteArrayOutputStream.)]
       (io/copy (io/input-stream x) out)
       (.toByteArray out))))

(def platform-data
  #?(:clj {::class File
           ::file (io/file "deps.edn")
           ::directory  (io/file ".")
           ::uri (URI. "https://github.com/djblue/portal")
           ::url (URL. "https://github.com/djblue/portal")
           ::exception (try (/ 1 0) (catch Exception e e))
           ::io-exception (try (slurp "/hello") (catch Exception e e))
           ::user-exception (Exception. "hi")
           ::uuid (UUID/randomUUID)
           ::date (java.util.Date.)
           ::binary (slurp-bytes "resources/screenshot.png")}
     :cljs {::promise (js/Promise.resolve 123)
            ::url (js/URL. "https://github.com/djblue/portal")
            ::uuid (random-uuid)
            ::date (js/Date.)}))

(def basic-data
  {::booleans #{true false}
   ::nil nil
   ::vector [1 2 4]
   "string-key" "string-value"
   ::list (range 3)
   ::set #{1 2 3}
   ::ns-symbol 'hello/world
   ::keyword :hello-world
   ::ns-keyword ::hello-world
   ::range (range 10)
   ::nested-vector [1 2 3 [4 5 6]]
   ::url-string "https://github.com/djblue/portal"})

(def clojure-data
  {::regex #"hello-world"
   ::var #'portal.colors/themes
   ::with-meta (with-meta 'with-meta {:hello :world})
   {:example/settings 'complex-key} :hello-world})

(def diff-data
  [{::removed "value"
    ::same-key "same-value"
    ::change-type #{1 2}
    ::deep-change {:a 0}
    ::set #{0 1 2}
    ::vector [::a ::removed ::b]
    ::different-value ::old-key}
   {::added "value"
    ::same-key "same-value"
    ::change-type {:a :b :c :d}
    ::deep-change {:a 1}
    ::set #{1 2 3}
    ::vector [::a ::added ::b]
    ::different-value ::new-key}])

(def hiccup
  [:div
   [:h1 "Hello, I'm hiccup"]
   [:a {:href "https://github.com/djblue/portal"} "djblue/portal"]])

(def data
  {::platform-data platform-data
   ::hacker-news hn/stories
   ::diff diff-data
   ::basic-data basic-data
   ::themes c/themes
   ::clojure-data clojure-data
   ::markdown (read-file "README.md")
   ::hiccup hiccup})
