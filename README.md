# portal

A clojure tool to navigate through your data.

[![Clojars Project](https://img.shields.io/clojars/v/djblue/portal?color=2a2e39&style=for-the-badge)](https://clojars.org/djblue/portal)

[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23portal-4A154B?logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/portal)

[![screenshot](https://raw.githubusercontent.com/djblue/portal/master/resources/screenshot.png)](http://djblue.github.io/portal/)

The portal UI can be used to inspect values of various shapes and sizes. The UX will probably evolve over time and user feedback is welcome!

## Apropos Demo

To get an overview of portal, you can watch the following recording of a live demo I gave on [Apropos](https://www.youtube.com/channel/UC1UxEQuBvfLJgWR5tk_XIXA/featured).

[![Apropos](https://img.youtube.com/vi/gByyg-m0XOg/hqdefault.jpg)](https://www.youtube.com/watch?v=gByyg-m0XOg "Apropos")

## Usage

To start a repl with portal, run the clojure cli with:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.8.0"}}}'
```

or for a **web** clojurescript repl, do:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.8.0"}
                    org.clojure/clojurescript {:mvn/version "1.10.758"}}}' \
    -m cljs.main
```

or for a **node** clojurescript repl, do:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.8.0"}
                    org.clojure/clojurescript {:mvn/version "1.10.758"}}}' \
    -m cljs.main -re node
```

or for a [**babashka**](https://github.com/borkdude/babashka) [>=0.2.4](https://github.com/borkdude/babashka/blob/master/CHANGELOG.md#v024) repl, do:

```bash
bb -cp `clj -Spath -Sdeps '{:deps {djblue/portal {:mvn/version "0.8.0"}}}'`
```

or for examples on how to integrate portal into an existing project, look through the [examples](./examples) directory. 

### API

Try the [portal api](./src/portal/api.cljc) with the following commands:

```clojure
;; for node and jvm
(require '[portal.api :as p])

;; for web
;; NOTE: you might need to enable popups for the portal ui to work in the
;; browser.
(require '[portal.web :as p])



(p/open) ; Open a new inspector

(add-tap #'p/submit) ; Add portal as a tap> target

(tap> :hello) ; Start tapping out values

(p/clear) ; Clear all values

(tap> :world) ; Tap out more values

(remove-tap #'p/submit) ; Remove portal from tap> targetset

(p/close) ; Close the inspector when done
```

### Portal Atom

For the `jvm`, `bb` and `web` platforms, you can pull values from portal back
into your runtime by treating portal as an atom:

```clojure
(def a (p/open))

; push the value 0 into portal
(reset! a 0)

@a ;=> 0

; inc the current value in portal
(swap! a inc)

@a ;=> 1
```

The currently selected viewer has the ability to intercept and transform
values returned by `deref`. For example, given a map in portal, you may
decide to view it as a coll, and with that viewer selected, `deref` would
return a list of pairs. Not many viewers implement this functionality
currently, but expect more to do so in the future.

### Options

#### Themes

There are currently three built-in themes:

- [`:portal.colors/nord`](https://www.nordtheme.com/) (default)
- [`:portal.colors/solarized-dark`](https://ethanschoonover.com/solarized/)
- [`:portal.colors/solarized-light`](https://ethanschoonover.com/solarized/)

Which can be passed as an option to `p/open`:

```clojure
(p/open
  {:portal.colors/theme :portal.colors/nord})
```

#### Launcher

By default, when `p/open` is called, an HTTP server is started on a randomly
chosen port. To control this server's port and host, call the `p/start`
function with the following options:

| Option                  | Description                | If not specified     |
|-------------------------|----------------------------|----------------------|
| `:portal.launcher/port` | Port used to access UI     | random port selected |
| `:portal.launcher/host` | Hostname used to access UI | "localhost"          |

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

Add a portal alias in `~/.clojure/deps.edn`

```clojure
:portal/cli
{:main-opts ["-m" "portal.main"]
 :extra-deps
 {djblue/portal {:mvn/version "0.8.0"}
  ;; optional json support
  cheshire/cheshire {:mvn/version "5.10.0"}
  ;; optional yaml support
  clj-commons/clj-yaml {:mvn/version "0.7.0"}}}
```

Then do the following depending on your data format:

```bash
cat data | clojure -M:portal/cli [edn|json|transit|yaml]
# or with babashka for faster startup
cat data | bb -cp `clojure -Spath -M:portal/cli` -m portal.main [edn|json|transit|yaml]
```

I keep the following bash aliases handy for easier CLI use:

```bash
alias portal='bb -cp `clojure -Spath -M:portal/cli` -m portal.main'
alias edn='portal edn'
alias json='portal json'
alias transit='portal transit'
alias yaml='portal yaml'
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

### vim + [vim-fireplace](https://github.com/tpope/vim-fireplace)

To start the nrepl server, do:

    make dev

vim-fireplace should automatically connect upon evaluation, but this will
only be for clj files, to get a cljs repl, do:

    :CljEval (user/cljs)

### emacs + [cider](https://cider.mx/)

The best way to get started via emacs is to have cider start the repl, do:

    M-x cider-jack-in-clj&cljs

[.dir-locals.el](./.dir-locals.el) has all the configuration variables for
cider.

### [`user.clj`](dev/user.clj)

The user.clj namespace has a bunch of useful examples and code for
development. Take a peek to get going.

### Formatting

To format source code, do:

    make fmt

### CI Checks

To run all ci checks, do:

    make ci

### E2E Testing

To run the e2e tests in the jvm, node and web environments, do:

    make e2e

NOTE: these aren't fully automated tests. They depend on a human for
verification and synchronization but it beats having to type everything
out manually into a repl.

### Deployment

To deploy to a release to [clojars](https://clojars.org/djblue/portal),
bump the [version](https://github.com/djblue/portal/blob/master/Makefile#L5) and do:

    make set-version deploy
