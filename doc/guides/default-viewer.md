# Default Viewer

If you would like to add some logic around which viewer to use by default in
Portal, the easiest way is to provide a `:portal.viewer/default` key as part of
a value's metadata. Unfortunately, not all values support metadata, such as
strings. They way around this limitation is to wrap the value in a container
that does support metadata, such as vectors. However, instead of managing this
manually, we can leverage the `portal.viewer` ns to help provide default viewers
for values.

Here is one such default viewer selector:

```clojure
(require '[portal.viewer :as v])

(def defaults
  {string? v/text
   bytes?  v/bin})

(defn- get-viewer-f [value]
  (or (some (fn [[predicate viewer]]
              (when (predicate value)
                viewer))
            defaults)
      v/tree))
```

With a corresponding submit function:

```clojure
(require '[portal.api :as p])

(defn submit [value]
  (let [f (get-viewer-f value)]
    (p/submit (f value))))

(add-tap #'submit)
```

Now by tapping the following values you can see the default viewer selector in
action:

```clojure
(tap> "hello, world")
(tap> (byte-array [0 1 2 3]))
(tap> (range 10))
```
