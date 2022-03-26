# Intellij

The Intellij plugin provides a way to embed the Portal UI directly into your IDE.

[![Screenshot](https://user-images.githubusercontent.com/1986211/140680825-431459a8-02d5-40f8-b71c-42aa026cfe93.png)](https://plugins.jetbrains.com/plugin/18467-portal-inspector/)

You can download the IntelliJ plugin from the [Jet Brains
Marketplace](https://plugins.jetbrains.com/plugin/18467-portal-inspector/).
"Portal" button will appear on the right-hand bar. The window area will be blank
until portal is launched from the REPL.

Add a dependency on Portal to your `deps.edn` or `project.clj`, start a Clojure
REPL and inside that evaluate:

```clojure
(do
    (def user/portal ((requiring-resolve 'portal.api/open) {:launcher :intellij}))
    (add-tap (requiring-resolve 'portal.api/submit)))
```

You can now `tap>` data and they will appear in the Portal tool window.

## Features

The main benefits of using this plugin are:

- Automatic font / theme discovery.
- No window management.
- Editor specific commands. See: goto-definition in the command palette.

## Debugging

If after running `portal.api/open` at the REPL, the Portal UI does not open, it
is most likely due to having a multi-module project. When the plugin is started,
it writes a `.portal/intellij.edn` file, which the REPL process will try to
find. If the REPL process is started outside of the root project, it will not be
able to use the Intellij plugin.

A quick hack to get around this problem is to symlink the `.portal` directory to
the directory where the REPL process is started.
