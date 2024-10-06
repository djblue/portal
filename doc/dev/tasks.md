# Dev Tasks

## Formatting

To format source code, do:

```bash
bb fmt
```

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

> [!NOTE]
> these aren't fully automated tests. They depend on a human for verification
> and synchronization but it beats having to type everything out manually into
> a repl.

## Extensions

To build the [vs-code](./extension-vscode) and [intellij](./extension-intellij) extensions, do:

```bash
bb ext
```

### VS Code Dev

To launch a dev version of the vs-code extension, open a new directory with
[extension-vscode](../../extension-vscode/) as the root. Then under the `Run and
Debug` tab you should see a `Run Portal` action which will launch a new vs-code
dev instance, I like to open the [root](../../) Portal directory in this
instance. You can also connect to shadow-cljs via the `:vs-code` or
`:vs-code-notebook` builds to eval code at the repl. [Here][shadow-cljs] are the
editor specific shadow-cljs guides.

## Deployment

To deploy to a release to [clojars](https://clojars.org/djblue/portal), do:

```bash
bb tag
bb deploy
```

[shadow-cljs]: https://shadow-cljs.github.io/docs/UsersGuide.html#_editor_integration