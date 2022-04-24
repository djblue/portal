# JavaScript Promises

In a JavaScript context, you often need to deal with asynchronicity and this
often involves promises. However, when working with promises in ClojureScript,
getting at the value of a promise can be a bit tedious. I often see the
following pattern for `println` debugging.

```clojure
(defn async-fn []
  (.resolve js/Promise :hello))

(.then (async-fn) println)
```

I'm not a huge fan of this pattern, and worse yet, if you forget to wrap the
value, you get `#object [Promise [object Promise]]` which isn't very helpful.

Thankfully, in ClojureScript you have
[`tap>`](https://clojuredocs.org/clojure.core/tap%3E) which can help you with
this issue.

With portal, you would typically do something like:

```clojure
(require '[portal.api :as p])
(add-tap #'p/submit)

(tap> (async-fn)) ;; #object [Promise [object Promise]]
```

But this has the same issue as before when you printed the promise value. To
improve `tap>` usage around promises, you simply need to provide a specialized
tap target, like the following:

```clojure
(require '[portal.api :as p])

(defn async-submit [value]
  (if-not (instance? js/Promise value)
    (p/submit value)
    (.then value p/submit)))

(add-tap #'async-submit)

(tap> (async-fn)) ;; :hello
```
