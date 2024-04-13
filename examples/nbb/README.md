# nbb + portal

To start using portal with [nbb][nbb], you just need to include the following in
your `nbb.edn`:

```clojure
;; nbb.edn
{:paths ["dev"]
 :deps {djblue/portal {:mvn/version "0.55.1"}}}
```

Then after connecting to a REPL, you should be able to follow the [API guide][api].

## Auto Start

Additionally, you can automatically start portal and nrepl with the following
code:

```clojure
;; dev/start.cljs
(ns start
  (:require [nbb.nrepl-server :as n]
            [nbb.repl :as r]
            [portal.api :as p]))

(defn async-submit [value]
  (if-not (instance? js/Promise value)
    (p/submit value)
    (-> value
        (.then p/submit)
        (.catch p/submit))))

(defn -main []
  (add-tap #'async-submit)
  (n/start-server! {:port 1337})
  (p/open {:launcher :vs-code})
  (r/repl))
```

Which can then automatically be wired into your package.json:

```javascript
// package.json
{
  "scripts": {
    "start": "nbb -m start"
  },
  "dependencies": {
    "nbb": "^1.2.187"
  }
}
```

And then start everything via npm:

```bash
npm start
```

## Remote Client

If you would like to send data to another remote Portal, you can use the
`portal.client.node` client to leverage the [Remote API][remote].

[nbb]: https://github.com/babashka/nbb/
[api]: ../../README.md#api
[remote]: ../../doc/remote-api.md#usage