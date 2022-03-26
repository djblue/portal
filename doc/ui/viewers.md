# Viewers

A viewer takes a raw value and renders it to the screen. A single value can have
many viewers. Most viewers have a `:predicate` function to define what values
they support. A `:predicate` can be as simple as a type check to as complex as a
[`clojure.spec.alpha/valid?`](https://clojuredocs.org/clojure.spec.alpha/valid_q)
assertion. The bottom-left dropdown displays the viewer for the currently
selected value and contains all viewers for the value.

A default viewer can be set via metadata. For example:

```clojure
^{:portal.viewer/default :portal.viewer/hiccup} [:h1 "hello, world"]
