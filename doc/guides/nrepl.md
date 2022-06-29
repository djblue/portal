# nREPL

If you would like to send every REPL eval to Portal, you can use the
`portal.nrepl/wrap-portal` nrepl middleware.

**NOTE:** Portal will keep all evaluated objects from being garbage collected
until they are cleared from the UI.

## tools.deps

If you are starting nrepl from tools.deps, you can try the following:

```clojure
;; deps.edn
{:aliases
 {:nrepl
  {:extra-deps {cider/cider-nrepl {:mvn/version "0.28.5"}}
   :main-opts ["-m" "nrepl.cmdline"
               "--middleware"
               "[cider.nrepl/cider-middleware,portal.nrepl/wrap-portal]"]}}}
```

## shadow-cljs

If you are using shadow-cljs, you can add the middleware via the
`shadow-cljs.edn` file:

```clojure
;; shadow-cljs.edn
{:nrepl {:middleware [portal.nrepl/wrap-portal]}}
```
