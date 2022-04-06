# Dev Tasks

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
