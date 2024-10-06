# portal.console

A major advantage of [`tap>`](https://clojuredocs.org/clojure.core/tap%3E) over
[`println`](https://clojuredocs.org/clojure.core/println) is that it allows you
to keep all values as data. However, as with `println`, if you `tap>` too many
values, it is very easy to lose track where those values came from. Luckily,
there is an easy fix, clojure macros.

The `portal.console` namespace has a handful of macros that will not only `tap>`
values, but capture the context in which those values are tapped. This includes
source code, time and runtime information. Additionally, the data is formatted
to work with the `:portal.viewer/log` viewer.

## Example

```clojure
;; user.clj
(require '[portal.console :as log])

(log/trace ::trace)
(log/debug ::debug)
(log/info  ::info)
(log/warn  ::warn)
(log/error ::error)
```

Will produce the following:

![logs](https://user-images.githubusercontent.com/1986211/196558924-d07fa896-2550-427e-b437-9a6f83fba1fb.png)

> [!NOTE]
> The data produced by `portal.console` is compatible with the
> `portal.runtime.jvm.editor/goto-definition` command which can automatically
> open the file location via the `:launcher` / `:editor` option passed to
> `portal.api/open`.

## Spec

If you could like the generate data for the log viewer in another context, the
following specs will be useful:

```clojure
(def levels
  [:trace :debug :info :warn :error :fatal :report])

(s/def ::level (set levels))

(s/def ::ns symbol?)
(s/def ::time inst?)

(s/def ::column int?)
(s/def ::line int?)

(s/def ::log
  (s/keys :req-un
          [::level
           ::ns
           ::time
           ::line
           ::column]))
```
