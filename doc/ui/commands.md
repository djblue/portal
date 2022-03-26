# Commands

The bottom-right yellow button will open the command palette. Commands can have
a `:predicate` function like viewers, so only relevant commands will be visible
which is based on the currently selected value. They will be sorted
alphabetically by name and can quickly be filtered. The `(ctrl | ⌘) + shift +
p` or `ctrl + j` shortcuts can also be used to open the command palette.

The filter string is split by white space and all words must appear in a name to
be considered a match.

To register your own command, use the `portal.api/register!` function. For example:

``` clojure
(portal.api/register! #'identity)
```

When multiple values are selected, commands will be applied as follows:

``` clojure
(apply f [first-selected second-selcted ...])
```

**NOTES:**

- A very useful command is `portal.ui.commands/copy` which will copy the
  currently selected value as an edn string to the clipboard.
- [`lambdaisland.deep-diff2/diff`](https://github.com/lambdaisland/deep-diff2#use)
  is a useful command for diffing two selected values.
- Commands manipulating the UI itself will live under the `portal.ui.commands`
  namespace.
- The `cljs.core` namespace will be aliased as `clojure.core` when using a
  clojurescript runtime.
