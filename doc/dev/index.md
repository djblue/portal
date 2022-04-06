# Development

After [installing dependencies](./deps.md) and
[setting up an editor](./editors.md), the following should be enough to get you
started with Portal development.

## [`user.clj`](../../dev/user.clj)

A good place to start poking around is in the [user](../../dev/user.clj)
namespace. It has a bunch of useful example code for development. Take a peek to
get going, but here are a few important bits.

## `:mode` `:dev`

By default, anytime a Portal window is opened, it loads the production UI. If
you intend to edit the [UI](../../src/portal/ui) code, you will need to start
Portal with the following option:

```clojure
(portal.api/open {:mode :dev})
```
