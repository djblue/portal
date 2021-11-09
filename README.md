# portal

A clojure tool to navigate through your data.

[![Clojars Project](https://img.shields.io/clojars/v/djblue/portal?color=380036&style=flat-square)](https://clojars.org/djblue/portal)
[![VS Code Extension](https://vsmarketplacebadge.apphb.com/version-short/djblue.portal.svg?color=007ACC&label=vs-code&logo=vs&style=flat-square)](https://marketplace.visualstudio.com/items?itemName=djblue.portal)
[![Get help on Slack](https://img.shields.io/badge/slack-clojurians%20%23portal-4A154B?color=63B132&style=flat-square)](https://clojurians.slack.com/channels/portal)

[![screenshot](https://user-images.githubusercontent.com/1986211/129153169-4018d586-d747-48f9-8193-d267ea5e288a.png)](https://djblue.github.io/portal/)

The portal UI can be used to inspect values of various shapes and sizes. The UX
will probably evolve over time and user feedback is welcome!

For an in-depth explanation of the UX, jump to [UX Concepts](#ux-concepts).

## Apropos Demo

To get an overview of portal, you can watch the following recording of a [live demo](https://www.youtube.com/watch?v=gByyg-m0XOg) I gave on [Apropos](https://www.youtube.com/channel/UC1UxEQuBvfLJgWR5tk_XIXA/featured).

[![Apropos](https://img.youtube.com/vi/gByyg-m0XOg/hqdefault.jpg)](https://www.youtube.com/watch?v=gByyg-m0XOg "Apropos")

## Usage

To start a repl with portal, run the **clojure >= 1.10.0** cli with:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.17.0"}}}'
```

or for a **web** **clojurescript >= 1.10.773** repl, do:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.17.0"}
                    org.clojure/clojurescript {:mvn/version "1.10.844"}}}' \
    -m cljs.main
```

or for a **node** **clojurescript >= 1.10.773** repl, do:

```bash
clj -Sdeps '{:deps {djblue/portal {:mvn/version "0.17.0"}
                    org.clojure/clojurescript {:mvn/version "1.10.844"}}}' \
    -m cljs.main -re node
```

or for a **babashka >=0.2.4** repl, do:

```bash
bb -cp `clj -Spath -Sdeps '{:deps {djblue/portal {:mvn/version "0.17.0"}}}'`
```

or for examples on how to integrate portal into an existing project, look through the [examples](./examples) directory.

**NOTE** Portal can also be used without a runtime via the [standalone
version](https://djblue.github.io/portal/).

### API

Try the [portal api](./src/portal/api.cljc) with the following commands:

```clojure
;; for node and jvm
(require '[portal.api :as p])

;; for web
;; NOTE: you might need to enable popups for the portal ui to work in the
;; browser.
(require '[portal.web :as p])


(def p (p/open)) ; Open a new inspector

;; or with an extension installed, do:
(def p (p/open {:launcher :vs-code}))  ; JVM only for now
(def p (p/open {:launcher :intellij})) ; JVM only for now

(add-tap #'p/submit) ; Add portal as a tap> target

(tap> :hello) ; Start tapping out values

(p/clear) ; Clear all values

(tap> :world) ; Tap out more values

(prn @p) ; bring selected value back into repl

(remove-tap #'p/submit) ; Remove portal from tap> targetset

(p/close) ; Close the inspector when done
```

**NOTE**: portal will keep objects from being garbage collected until they are
cleared from the UI.

### Portal Atom

In addition to getting the selected value back in the repl, the portal atom also
allows direct manipulation of portal [history](#history). For example:

```clojure
(def a (p/open))

; push the value 0 into portal history
(reset! a 0)

@a ;=> 0 - get current value

; inc the current value in portal
(swap! a inc)

@a ;=> 1 - get current value
```

### Options

#### Themes

There are currently three built-in themes:

- [`:portal.colors/nord`](https://www.nordtheme.com/) (default)
- [`:portal.colors/solarized-dark`](https://ethanschoonover.com/solarized/)
- [`:portal.colors/solarized-light`](https://ethanschoonover.com/solarized/)
- [`:portal.colors/zerodark`](https://github.com/NicolasPetton/zerodark-theme)

Which can be passed as an option to `p/open`:

```clojure
(p/open
  {:portal.colors/theme :portal.colors/nord})
```

#### Launcher

By default, when `p/open` is called, an HTTP server is started on a randomly
chosen port. It is also given a default window title of the form `portal - <platform> - <version>`.
To control this server's port, host, and window title, call the `p/start`
function with the following options:

| Option                          | Description                | If not specified     |
|---------------------------------|----------------------------|----------------------|
| `:portal.launcher/port`         | Port used to access UI     | random port selected |
| `:portal.launcher/host`         | Hostname used to access UI | "localhost"          |
| `:portal.launcher/app`          | Launch as separate window  | true                 |
| `:portal.launcher/window-title` | Custom title for UI window | "portal"             |

## UX Concepts

The portal ux can be broken down into the following components:

### Selection

A single click will select a value. The arrow keys, (`⭠` `⭣` `⭡` `⭢`) or (`h`
`j` `k` `l`) will change the selection relative to the currently selected value.
Relative selection is based on the viewer.

### Viewers

A viewer takes a raw value and renders it to the screen. A single value can have
many viewers. Most viewers have a `:predicate` function to define what values
they support. A `:predicate` can be as simple as a type check to as complex as a
[`clojure.spec.alpha/valid?`](https://clojuredocs.org/clojure.spec.alpha/valid_q)
assertion. The bottom-left dropdown displays the viewer for the currently
selected value and contains all viewers for the value.

A default viewer can be set via metadata. For example:

```clojure
^{:portal.viewer/default :portal.viewer/hiccup} [:h1 "hello, world"]
```

### Commands

The bottom-right yellow button will open the command palette. Commands can have
a `:predicate` function like viewers, so only relevant commands will be visible
which is based on the currently selected value. They will be sorted
alphabetically by name and can quickly be filtered.

The filter string is split by white space and all words must appear in a name to
be considered a match.

To register your own command, use the `portal.api/register!` function. For example:

``` clojure
;; Currently, only single arity vars can be registerd as commands.
(portal.api/register! #'identity)
```

**NOTES:**

- Commands manipulating the UI itself will live under the `portal.ui.commands`
  namespace.
- A very useful command is `portal.ui.commands/copy` which will copy the
  currently selected value as an edn string to the clipbaord.
- The `cljs.core` namespace will be aliased as `clojure.core` when using a
  clojurescript runtime.

### History

The top-left arrow buttons (`⭠` `⭢`) can be used to navigate through history.
History is built up from commands pushing values into it. For example, anytime a
value is double clicked,
[`clojure.datafy/nav`](https://clojuredocs.org/clojure.datafy/nav) is applied to
that value and the result is pushed onto the history stack. All commands that
produce a new value will do the same.

To quickly navigation through history, checkout the following commands:

- `portal.ui.commands/history-back`
- `portal.ui.commands/history-forward`
- `portal.ui.commands/history-first`
- `portal.ui.commands/history-last`

### Shortcuts

To run a command without having to open the command palette, you can use the
commands shortcut. They will be listed in the command palette next to the
command. When multiple shortcuts are available, they are separated by a vertical
bar.

**NOTE:** shortcuts aren't currently user definable.

### Filtering

Like many concepts listed above, filtering is relative to the currently selected
value. If no value is selected, filtering is disabled. When a collection is
selected, the filter text will act to remove elements from that collection,
similar to the command palette.

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
 {djblue/portal {:mvn/version "0.17.0"}
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

There is also the ability to invoke a standalone http server to listen and
display data from remote client

```bash
   bb -cp `clojure -Spath -Sdeps '{:deps {djblue/portal {:mvn/version "LATEST"}}}'` \
      -e '(require (quote [portal.api])) (portal.api/open {:portal.launcher/port 53755}) @(promise)'
```

## Editor Integration

### Emacs

If you are an emacs + cider user and would like tighter integration with portal,
the following section may be of interest to you.

``` emacs-lisp
;; Leverage an existing cider nrepl connection to evaluate portal.api functions
;; and map them to convenient key bindings.

(defun portal.api/open ()
  (interactive)
  (cider-nrepl-sync-request:eval
   "(require 'portal.api) (portal.api/tap) (portal.api/open)"))

(defun portal.api/clear ()
  (interactive)
  (cider-nrepl-sync-request:eval "(portal.api/clear)"))

(defun portal.api/close ()
  (interactive)
  (cider-nrepl-sync-request:eval "(portal.api/close)"))

;; Example key mappings for doom emacs
(map! :map clojure-mode-map
      ;; cmd  + o
      :n "s-o" #'portal.api/open
      ;; ctrl + l
      :n "C-l" #'portal.api/clear)

;; NOTE: You do need to have portal on the class path and the easiest way I know
;; how is via a clj user or project alias.
(setq cider-clojure-cli-global-options "-A:portal")
```

### VS Code

If you are using vs-code, try out the
[vs-code-extension](https://marketplace.visualstudio.com/items?itemName=djblue.portal).
It allows launching portal in an embedded
[webview](https://code.visualstudio.com/api/extension-guides/webview) within
vs-code.

For a more in depth look at customizing vs-code for use with portal,
particularly with
[mauricioszabo/clover](https://github.com/mauricioszabo/clover), take a look at
[seancorfield/vscode-clover-setup](https://github.com/seancorfield/vscode-clover-setup).

**NOTE:** The version of portal being run in the webview is still decided by the
runtime in which `(portal.api/open {:launcher :vs-code})` is run.

## Principles

- Support as much of clojure's data as possible
- First class support for async extensibility
- Simple standalone usage without a clojure environment
- Easy theming

## Prior Art

- [Clouseau](https://common-lisp.net/project/mcclim/static/manual/mcclim.html)
  - [Demo Video](https://youtu.be/-1LzFxTbU9E)
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
- ~Chart Viewer~
  - Initial chart viewers [#31](https://github.com/djblue/portal/pull/31) using [vega-lite](https://vega.github.io/vega-lite/).
- Node+Edge Graphs Viewer

## Development

### Dependencies

To start the development server, make sure you have the following dependencies
installed on your system:

- [babashka](https://babashka.org/) - for build scripting
  - for osx, do: `brew install borkdude/brew/babashka`
  - to list all build tasks, do: `bb tasks`
- [node, npm](https://nodejs.org/) - for javascript dependencies
  - for osx, do: `brew install node`

### vim + [vim-fireplace](https://github.com/tpope/vim-fireplace)

To start the nrepl server, do:

```bash
bb dev
```

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

To launch the dev client version of portal, make sure to do:

```clojure
(portal.api/open {:mode :dev})
```

### Formatting

To format source code, do:

    bb fmt

### CI Checks

To run ci checks, do:

```bash
bb ci    # run all ci check

bb check # run just the static analysis
bb test  # run just the tests
```

### E2E Testing

To run the e2e tests in the jvm, node and web environments, do:

```bash
bb e2e
```

**NOTE:** these aren't fully automated tests. They depend on a human for
verification and synchronization but it beats having to type everything out
manually into a repl.

### Extensions

To build the [vs-code](./extension-vscode) and [intellij](./extension-intellij) extensions, do:

```bash
bb ext
```

### Deployment

To deploy to a release to [clojars](https://clojars.org/djblue/portal), do:

```bash
bb tag
bb deploy
```
