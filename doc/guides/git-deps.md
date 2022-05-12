# Portal Git Dep

To use Portal as a [git dep](https://clojure.org/news/2018/01/05/git-deps), you
should add an alias like the following to your `deps.edn`:

```clojure
:aliases
{:dev
 {:extra-deps
  {djblue/portal
   {:git/url "https://github.com/djblue/portal.git"
    :sha "1645d580f7487657991ad112381104c133342faa"}}}}
```

Or using the [newer format](https://clojure.org/guides/deps_and_cli#_using_git_libraries):

```clojure
:aliases
{:dev
 {:extra-deps
  {io.github.djblue/portal
   {:git/tag "0.25.0" :git/sha "1645d58"}}}}
```

However, this will fail to resolve with the following error:

```
Checking out: https://github.com/djblue/portal.git at 1645d580f7487657991ad112381104c133342faa
Error building classpath. The following libs must be prepared before use: [io.github.djblue/portal]

```

To use Portal from source, you need to perform a build step for the UI. The UI
build assumes you have [node.js](https://nodejs.org/) available on your system.

Now, to prepare Portal from source, use the following `clj` command:

```bash
clj -X:deps prep :aliases '[:dev]'
```

That's it, you should be able to use Portal!
