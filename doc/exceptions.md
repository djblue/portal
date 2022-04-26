# Exceptions

Raw exception objects show up like any other object in Portal, like it would be
printed at the REPL.

However, Portal has a special viewer for exceptions and the good news is that
it's based on the
[`clojure.datafy/datafy`](https://clojuredocs.org/clojure.datafy/datafy) of
exceptions in Clojure.

To get things wired up so exceptions are auto data-fied before being sent to
Portal, try the following:


```clojure
(ns user
  (:require [portal.api :as p]
            [clojure.datafy :as d]))

(defn error->data [ex]
  (assoc (d/datafy ex) :runtime :clj))

(defn submit [value]
  (p/submit
   (if-not (instance? Exception value)
     value
     (error->data value))))

(add-tap #'submit)

(tap> (ex-info "My Error!!!" {:my :data}))
```

Unfortunately, `js/Error` isn't data-fied in ClojureScript, however, you can
provide your own mapping between errors and data.

Here is one such mapping and submit function:

```
(defn error->data [ex]
  (merge
   (when-let [data (.-data ex)]
     {:data data})
   {:runtime :cljs
    :cause   (.-message ex)
    :via     [{:type    (symbol (.-name (type ex)))
               :message (.-message ex)}]
    :stack   (.-stack ex)}))

(defn submit [value]
  (p/submit
   (if-not (instance? js/Error value)
     value
     (error->data value))))

(add-tap #'submit)

(tap> (ex-info "My Error!!!" {:my :data}))
```
