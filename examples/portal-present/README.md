# Custom Viewer

Portal is implemented with [reagent][reagent] so implementing a custom viewer is
as simple as writing a reagent component, this should be relatively easy
assuming you are familiar with reagent.

In this guide I will walk you through creating a slide deck viewer in Portal.

## Portal nREPL

If you would like to interactively develop your custom viewer, and are using
nREPL, you can use the `portal.nrepl/wrap-repl` middleware. With that middleware
enabled, you can launch a sub repl with the following:

```clojure
(require '[portal.api :as p])
(def portal (p/open))
;; Turns the current nREPL session into a portal repl session.
(p/repl portal)
```

Now all subsequent eval invocations will run in the Portal UI ClojureScript
runtime.

> [!NOTE]
> To quit the Portal repl, eval `:cljs/quit`. This will automatically
> occur if the portal session is no longer available.

## Input Data Structure

For this particular viewer, we can enable any collection to act as a slide deck
and allow each element in the sequence to decide how it wants to be rendered.

For example, the following should be considered a valid slide deck:

```clojure
[^{:portal.viewer/default :portal.viewer/hiccup}
 [:h1 "hello"]
 ^{:portal.viewer/default :portal.viewer/hiccup}
 [:h1 "world"]]
```

Let's express this as a `:predicate` for later use:

```clojure
(defn deck? [value] (and (coll? value) (not (map? value))))
```

## Creating a Reagent Component

The only assumption that Portal makes about viewer components is that they take
a value as their first argument, everything else is up to the component.

For example, the following is a valid Portal viewer:

```clojure
(defn viewer [value] [:pre {:style {:color :pink}} (pr-str value)])
```

However, that's not very interesting. For the deck viewer, let's render the
currently selected element in the sequence as a slide:

```clojure
(ns portal-present.viewer
  (:require [portal.ui.api :as p]
            [portal.ui.inspector :as ins]
            [reagent.core :as r]))

(defn view-presentation []
  (let [slide (r/atom 0)]
    (fn [slides]
      [:<>
       [ins/inspector (nth (seq slides) @slide :no-slide)]
       [:button {:on-click #(swap! slide dec)} "prev"]
       [:button {:on-click #(swap! slide inc)} "next"]])))
```

## Registering a viewer

When you are satisfied with your component, you can register it with Portal via
`portal.ui.api/register-viewer!` such as the following:

```clojure
(portal.ui.api/register-viewer!
 {:name ::slides
  :predicate deck?
  :component view-presentation})
```

> [!NOTE]
> Anytime a viewer is registered, it will cause the UI to re-render. This
> is very handy for interactive development.

And with just that, you now have a fully working custom viewer!

## Code Loading

The next step, is getting the code loaded into the Portal UI runtime without
using `portal.api/repl`. This can be done via `portal.api/eval-str` which
enables evaluating arbitrary ClojureScript code in the Portal UI runtime from
the host runtime.

For example, the following will alert `1` when invoked at the host repl.

```clojure
(require '[portal.api :as p])
(p/eval-str "(js/alert 1)")
```

To load the code for an extension, we can provide the string directly or load it
from a file via slurp. For example:

```clojure
(require '[portal.api :as p])
(p/eval-str (slurp (io/resource "portal_present/viewer.cljs")))
```

> [!NOTE]
> you can specify which Portal instance you want this code evaluated in
> to prevent evaluating it in every available Portal UI runtime.

## Usage

Now that the viewer has been defined and loaded, you can select it like any viewer or enable it by default via metadata:

```clojure
(require '[portal.api :as p]
         '[portal.viewer :as-alias v])

(def slides
  ^{::v/default :portal-present.viewer/slides}
  [^{::v/default ::v/hiccup} [:h1 "hello"]
   ^{::v/default ::v/hiccup} [:h1 "world"]])

(add-tap p/submit)
(tap> slides)
```

## Auto Loading

An issue with the current loading mechanism is that is won't persist across
browser reloads or `portal.api/open` invocations. To address this issue, we can
auto load the viewer via the `:on-load` hook. For example:

```clojure
(require '[portal.api :as p])

(declare portal)

(defn on-load []
  (p/eval-str portal (slurp (io/resource "portal_present/viewer.cljs"))))

(def portal
  (p/open
   {:value slides
    :on-load on-load
    :window-title "Portal Present"}))
```

## Conclusion

With only the concepts above, you can now build a variety of custom viewers!
For a more complete example that builds on the above with improved styles and
UX, look at [portal-present.viewer](./src/portal_present/viewer.cljs).

[reagent]: https://reagent-project.github.io/
