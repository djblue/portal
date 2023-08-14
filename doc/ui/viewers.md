# Viewers

A viewer takes a value and renders it to the screen in a meaningful way. Since
a value can be visualized in various ways, there are usually multiple viewers to
choose from for a single value.

Viewers can have a `:predicate` function to define what values they support, so
viewers are automatically filtered based on the value.

The bottom-left dropdown displays the viewer for the currently
[selected](./selection.md) value and contains all supported viewers for that
value.

> **Note**
> You can also switch viewers via the `portal.ui.commands/select-viewer`
> [command](./commands.md).

## Default Viewers

Since the default priority order specified in Portal might not be desired, a
default viewer can be set via [metadata](https://clojure.org/reference/metadata).

For example:

```clojure
(tap> ^{:portal.viewer/default :portal.viewer/hiccup} [:h1 "hello, world"])
```

Or for a dynamic value:

```clojure
(def hiccup [:h1 "hello, world"])

(tap> (with-meta hiccup {:portal.viewer/default :portal.viewer/hiccup}))
```

## Tips

### String Values

For values that don't support metadata, such as strings, you can leverage the
hiccup viewer.

For example, given a markdown string, you could do the following:

```clojure
(def md-string "# hello, world")

(tap>
 (with-meta
   [:portal.viewer/markdown md-string]
   {:portal.viewer/default :portal.viewer/hiccup}))
```

The also illustrates an important aspect about the hiccup viewer, it support
traditional html and portal viewers as markup.
