# VS Code

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
