# Shadow CLJS

For [shadow-cljs](https://github.com/thheller/shadow-cljs) users, the easiest
way to get portal setup is via a
[preload](https://shadow-cljs.github.io/docs/UsersGuide.html#_preloads). It
allows you to inject a namespace for instrumentation during development.

> **Note** that this guide will focus on ClojureScript in the context of a web
> browser, but shadow-cljs supports other JavaScript environments.

## `portal.web` vs `portal.shadow.remote`

This first question you need to answer is, do you want Portal hosted by the web
page itself or do you want to send values to a remote instance using the
[remote-api](/doc/remote-api.md)?

The pros and cons of `portal.web` are:

- ✅ Access to native browser JavaScript objects
- ✅ Register [commands](/doc/ui/commands.md) in the browser JavaScript context
- ❌ State is linked to the browser page, so everything is cleared on page reload
- ❌ Can't be used within an [editor extension](/doc/editors/)

The pros and cons of `portal.shadow.remote` are:

- ✅ Values are persisted across page refreshes
- ✅ Can manipulate data in a runtime where you might have more powerful tools
- ❌ Native objects are serialized via pretty-print so you tend to have less
     leverage


> **Note** you can run both types at the same time without any issues.

## Setup

With that in mind, let's get started. In your `shadow-cljs.edn` file, you need
to add the following bit of configuration:

```clojure
;; shadow-cljs.edn
{:builds
 {:build-id
  {:target :browser
   ...
   :build-hooks [(portal.shadow.remote/hook)]
   :devtools {:preloads [portal.setup]}}}}
```

Or if you would like to pass options to `portal.api/start`:

```clojure
;; shadow-cljs.edn
{:builds
 {:build-id
  {:target :browser
   ...
   :build-hooks [(portal.shadow.remote/hook {:port 1234})]
   :devtools {:preloads [portal.setup]}}}}
```

### Web Setup

A basic setup with `portal.web` is as follows:

```clojure
(ns portal.setup
  (:require [portal.web :as p]))

;; Allows options to be propagated across page reloads
(p/set-defaults! {:theme :portal.colors/gruvbox})

(add-tap p/submit)
```

> **Note** To quickly open the Portal UI, you can use the `ctrl | cmd + shift +
> o` shortcut from the parent page.

### Remote Setup

A basic setup with `portal.shadow.remote` is as follows:

```clojure
(ns portal.setup
  (:require [portal.shadow.remote :as p]))

(add-tap p/submit)
```

### Custom Setup

For a more comprehensive setup, try the following which leverages both and some
additional bits from other guides:

```clojure
(ns portal.setup
  (:require [portal.shadow.remote :as r]
            [portal.web :as p]))

(defn- submit [value]
  (p/submit value)
  (r/submit value))

(defn- error->data [ex]
  (merge
   (when-let [data (.-data ex)]
     {:data data})
   {:runtime :portal
    :cause   (.-message ex)
    :via     [{:type    (symbol (.-name (type ex)))
               :message (.-message ex)}]
    :stack   (.-stack ex)}))

(defn- async-submit [value]
  (cond
    (instance? js/Promise value)
    (-> value
        (.then async-submit)
        (.catch (fn [error]
                  (async-submit error)
                  (throw error))))

    (instance? js/Error value)
    (submit (error->data value))

    :else
    (submit value)))

(add-tap async-submit)

(defn- error-handler [event]
  (tap> (or (.-error event) (.-reason event))))

(.addEventListener js/window "error" error-handler)
(.addEventListener js/window "unhandledrejection" error-handler)
```
