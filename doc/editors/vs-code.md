# VS Code

[![Screenshot](https://user-images.githubusercontent.com/1986211/140680881-497efd3b-da75-4220-9630-bf1af9d8bf37.png)](https://marketplace.visualstudio.com/items?itemName=djblue.portal)

If you are using vs-code, try out the [vs-code-extension][extension]. It allows
launching portal in an embedded [webview][webview] within vs-code.

For a more in depth look at customizing vs-code for use with portal,
particularly with [calva][calva], take a look at
[seancorfield/vscode-calva-setup][calva-setup].

For a more complete workflow guide, checkout [Calva, Joyride, and
Portal][guide] by [@seancorfield][seancorfield].

> **Note** The version of portal being run in the webview is still decided by
> the runtime in which `(portal.api/open {:launcher :vs-code})` is run.

[calva]: https://calva.io/
[calva-setup]: https://github.com/seancorfield/vscode-calva-setup
[guide]: https://corfield.org/blog/2022/12/18/calva-joyride-portal/
[seancorfield]: https://github.com/seancorfield
[extension]: https://marketplace.visualstudio.com/items?itemName=djblue.portal
[webview]: https://code.visualstudio.com/api/extension-guides/webview
