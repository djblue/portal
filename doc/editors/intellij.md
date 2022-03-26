# Intellij

Download the IntelliJ plugin from [Releases](https://github.com/djblue/portal/releases) and [install it from the disk](https://www.jetbrains.com/help/idea/managing-plugins.html).
A "Portal" button will appear on the right-hand bar.

Add a dependency on Portal to your project, start a Clojure REPL and inside that evaluate:

```clojure
(do
    (def user/portal ((requiring-resolve 'portal.api/open) {:launcher :intellij}))
    (add-tap (requiring-resolve 'portal.api/submit)))
```

You can now `tap>` data and they will appear in the Portal tool window.
