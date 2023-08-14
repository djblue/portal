# Editor Setup

For editor specific development tips, try the following:

## vim + [vim-fireplace](https://github.com/tpope/vim-fireplace)

To start a dev server, do:

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

[.dir-locals.el](../../.dir-locals.el) has all the configuration variables for
cider.

> **Note**
> When using cider-jack-in, you will need to manually `npm install`
> when the `package.json` or `package-lock.json` changes.
