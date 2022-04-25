# Themes

Portal has a handful of themes built-in that will be shown below. In the case of
[editor extensions](../editors), Portal will try match the theme of your editor.

The theme can be set via an option to `portal.api/open`:

```clojure
(p/open {:theme :portal.colors/nord})
```

Also, you can quickly toggle the theme via the
[`portal.ui.commands/set-theme`](./commands.md) command.

## Screenshots

### [`:portal.colors/nord`](https://www.nordtheme.com/) (default)

![portal nord theme](https://user-images.githubusercontent.com/1986211/165007021-fb4acae1-0128-45cf-94cd-ce26b456d456.png)

### [`:portal.colors/solarized-dark`](https://ethanschoonover.com/solarized/)

![portal solarized-dark theme](https://user-images.githubusercontent.com/1986211/165007028-4b37889a-bae4-490f-91c8-508f1b297856.png)

### [`:portal.colors/solarized-light`](https://ethanschoonover.com/solarized/)

![portal solaized-light theme](https://user-images.githubusercontent.com/1986211/165007035-adb3b88c-f20f-4509-a64d-a0fd4b783f62.png)

### `:portal.colors/material-ui`

![portal material-ui theme](https://user-images.githubusercontent.com/1986211/165007045-6d2be1bb-2b39-40bc-b4b4-7d2d54cb2a74.png)

### [`:portal.colors/zerodark`](https://github.com/NicolasPetton/zerodark-theme)

![portal zerodark theme](https://user-images.githubusercontent.com/1986211/165007055-1b8a4b27-8e2d-4d67-b5eb-55fe06fcbc9b.png)

### [`:portal.colors/gruvbox`](https://github.com/morhetz/gruvbox)

![portal gruvbox theme](https://user-images.githubusercontent.com/1986211/165007060-d5618a30-685c-42d7-88bb-35bd1b77a15a.png)
