# Clojure CLR + Portal

This project demonstrates how you can use Portal from Clojure CLR.

## Getting a REPL

To get a REPL started with Portal as a dependency, do:

```bash
make
```

or via a bash script, do:

```bash
./repl
```

This will pull do the following:

- Pulls the Portal source from github via the [clojure cli][clojure-cli]
- Builds Portal from source, which requires [node+npm][node+npm]
- Builds a class path via the [clojure cli][clojure-cli] saved to `.classpath`
- Starts `Clojure.Main` with `CLOJURE_LOAD_PATH` set to the class path from the
  previous step

You should now be able to follow the [api guide][portal-api] from the main README.

[clojure-cli]: https://clojure.org/guides/deps_and_cli
[node+npm]: https://nodejs.org/
[portal-api]: ../../README.md#api
