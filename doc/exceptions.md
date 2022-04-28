# Exceptions

![portal clojure exception viewer](https://user-images.githubusercontent.com/1986211/165203608-628715bc-7ed4-4e48-9002-08048137abb6.png)

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

```clojure
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

And with that you get the following:

![portal clojurescript exception viewer](https://user-images.githubusercontent.com/1986211/165203690-317d148b-962f-4579-a627-c187b7ae3a3e.png)

## Tip

If you would like to funnel all uncaught errors from a web based ClojureScript
app, consider adding the following handlers:

```clojure
(defn- error-handler [event]
  (tap> (or (.-error event) (.-reason event))))

(.addEventListener js/window "error" error-handler)
(.addEventListener js/window "unhandledrejection" error-handler)
```

Similarly for node, the following should work:

```clojure
(.on js/process "unhandledRejection" tap>)
(.on js/process "uncaughtException" tap>)
```
