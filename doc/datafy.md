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


## Tips

If you would like to automatically datafy all tapped values, try the following:

```clojure
(require '[clojure.datafy :as d])
(require '[portal.api :as p])

(def submit (comp p/submit d/datafy))
(add-tap #'submit)

(tap> *ns*)
```
