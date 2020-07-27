# portal

A clojure tool to navigate through your data.

[![Clojars Project](https://clojars.org/djblue/portal/latest-version.svg)](https://clojars.org/djblue/portal)

State: alpha, but usable

## Client

[![screenshot](https://raw.githubusercontent.com/djblue/portal/master/resources/screenshot.png)](http://djblue.github.io/portal/)

The portal UI can be used to inspect values of various shapes and sizes.
It will only show you one level at a time and clicking an item will
navigate you into that item. Simple values (`"hello, world"`, `:a`, `1`,
`true`) resolve to themselves, while compound values (`[]`, `{}`, `#{}`)
will contain their elements.

There are currently three viewers to see values: coll, map and table.
Coll supports any collection, while map only supports maps and the table
only supports list of maps.

The UX will probably evolve over time and user feedback is welcome!

## API Usage

To start a repl with portal, run the clojure cli with:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.1.1"}}}'
```

or for a node clojurescript repl, do:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.1.1"}
                    org.clojure/clojurescript {:mvn/version "1.10.758"}}}' \
    -m cljs.main -re node
```

then try the portal api with the following commands:

```clojure
(require '[portal.api :as p])

(p/open) ; Open a new inspector

(p/tap) ; Add portal as a tap> target

(tap> :hello) ; Start tapping out values

(p/clear) ; Clear all values

(tap> :world) ; Tap out more values

(p/close) ; Close the inspector when done
```

## Datafy and Nav

There is one exception to the behavior described above for the UI,
[datafy](https://clojuredocs.org/clojure.datafy/datafy) and
[nav](https://clojuredocs.org/clojure.datafy/nav). They are extension
points defined in clojure to support user defined logic for transforming
anything into clojure data and how to traverse it.

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

## CLI Usage

To use portal from the cli, do the following depending on your data
format:

```bash
cat deps.edn     | clojure -m portal.main edn
cat package.json | clojure -m portal.main json
cat transit.json | clojure -m portal.main transit
```

I keep the following aliases handy for easier CLI use:

```bash
alias edn='clojure -A:portal -m portal.main edn'
alias json='clojure -A:portal -m portal.main json'
alias transit='clojure -A:portal -m portal.main transit'
```

and often use the `Copy as cURL` feature in the chrome network tab to do
the following:

```
curl ... | transit
```

## Principles

- Support as much of clojure's data as possible
- First class support for async extensibility
- Simple standalone usage without a clojure environment
- Easy theming

## Prior Art

- [clojure.inspector](https://clojuredocs.org/clojure.inspector/inspect)
- [REBL](https://github.com/cognitect-labs/REBL-distro)
- [punk](https://github.com/Lokeh/punk)
- [shadow-cljs inspect](https://clojureverse.org/t/introducing-shadow-cljs-inspect/5012)

## Ideas for the Future 

- ~Diff Viewer~
  - Any vector pair can be diffed in portal via [lambdaisland/deep-diff2](https://github.com/lambdaisland/deep-diff2#diffing)
- ~Markdown Viewer~
  - Any string can be viewed as markdown in portal via [yogthos/markdown-clj](https://github.com/yogthos/markdown-clj)
  - Any hiccup data structure can also be viewed as html
- Chart Viewer
- Node+Edge Graphs Viewer

## Development

To start development, do:

    make dev

### Formatting

To format source code, do:

    make fmt

### CI Checks

To run all ci checks, do:

    make ci

## License

The MIT License (MIT)

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and associated documentation files (the "Software"),
to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense,
and/or sell copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
