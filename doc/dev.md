# Development

## Dependencies

To start the development server, make sure you have the following dependencies
installed on your system:

- [java](https://openjdk.java.net/) - for clojure runtime
  - for osx, do `brew install openjdk`
- [babashka](https://babashka.org/) - for build scripting
  - for osx, do: `brew install borkdude/brew/babashka`
  - to list all build tasks, do: `bb tasks`
- [node, npm](https://nodejs.org/) - for javascript dependencies
  - for osx, do: `brew install node`

## vim + [vim-fireplace](https://github.com/tpope/vim-fireplace)

To start the nrepl server, do:

```bash
bb dev
```

vim-fireplace should automatically connect upon evaluation, but this will
only be for clj files, to get a cljs repl, do:

```vim
:CljEval (user/cljs)
```

## emacs + [cider](https://cider.mx/)

The best way to get started via emacs is to have cider start the repl, do:

```bash
M-x cider-jack-in-clj&cljs
```


[.dir-locals.el](./.dir-locals.el) has all the configuration variables for
cider.

## [`user.clj`](dev/user.clj)

The user.clj namespace has a bunch of useful examples and code for
development. Take a peek to get going.

To launch the dev client version of portal, make sure to do:

```clojure
(portal.api/open {:mode :dev})
```

## Formatting

To format source code, do:

    bb fmt

## CI Checks

To run ci checks, do:

```bash
bb ci    # run all ci check

bb check # run just the static analysis
bb test  # run just the tests
```

## E2E Testing

To run the e2e tests in the jvm, node and web environments, do:

```bash
bb e2e
```

**NOTE:** these aren't fully automated tests. They depend on a human for
verification and synchronization but it beats having to type everything out
manually into a repl.

## Extensions

To build the [vs-code](./extension-vscode) and [intellij](./extension-intellij) extensions, do:

```bash
bb ext
```

## Deployment

To deploy to a release to [clojars](https://clojars.org/djblue/portal), do:

```bash
bb tag
bb deploy
```
