# [Datafy](https://clojuredocs.org/clojure.datafy/datafy) and [Nav](https://clojuredocs.org/clojure.datafy/nav)

Datafy and Nav are extension points defined in clojure to support user defined
logic for transforming anything into clojure data and how to traverse it.

For a great overview of datafy and nav, I recommend reading [Clojure
1.10's Datafy and Nav](https://corfield.org/blog/2018/12/03/datafy-nav/)
by Sean Corfield.

The below will demonstrate the power of datafy and nav by allowing you to
traverse the hacker news api! It will produce data tagged with metadata on
how to get more data!

```clojure
(require '[examples.hacker-news :as hn])

(tap> hn/stories)
```

An interesting use case for nav is allowing users to nav into keywords to
produce documentation for that keyword. This really highlights the power
behind datafy and nav. It becomes very easy to tailor a browser into the
perfect development environment!

Below is an example of extending the Datafiable protocol to java files:

```clojure
(require '[clojure.core.protocols :refer [Datafiable]])

(extend-protocol Datafiable
  java.io.File
  (datafy [^java.io.File this]
    {:name          (.getName this)
     :absolute-path (.getAbsolutePath this)
     :flags         (cond-> #{}
                      (.canRead this)     (conj :read)
                      (.canExecute this)  (conj :execute)
                      (.canWrite this)    (conj :write)
                      (.exists this)      (conj :exists)
                      (.isAbsolute this)  (conj :absolute)
                      (.isFile this)      (conj :file)
                      (.isDirectory this) (conj :directory)
                      (.isHidden this)    (conj :hidden))
     :size          (.length this)
     :last-modified (.lastModified this)
     :uri           (.toURI this)
     :files         (seq (.listFiles this))
     :parent        (.getParentFile this)}))
```

Here is an example how to leverage [protocol extension via metadata](https://clojure.org/reference/protocols#_extend_via_metadata) to make it possible to navigate to a related entity, namely from a book to its author:

```clojure
;; in practice you'd use e.g. next.jdbc with a real DB, here we've a db map:
(let [db {:book [#:book{:id 1, :title "1984" :author 10}]
            :person [#:person{:id 10 :fname "George" :lname "Orwell"}]}]
    (tap> (->> (get db :book)
               (map #(with-meta % {`clojure.core.protocols/nav
                                   (fn [_coll key value]
                                     (if (= key :book/author)
                                       (first (filter (comp #{value} :person/id) (:person db)))
                                       value))})))))
```

This is what it looks like. Notice the book author's ID is selected - if we press Enter now, it will trigger the navigation and display the person map:

![portal-nav-example](https://user-images.githubusercontent.com/624958/198690804-269131fe-2c77-4c37-96b2-fd4d6831d452.png)


## Tips

If you would like to automatically datafy all tapped values, try the following:

```clojure
(require '[clojure.datafy :as d])
(require '[portal.api :as p])

(def submit (comp p/submit d/datafy))
(add-tap #'submit)

(tap> *ns*)
```
