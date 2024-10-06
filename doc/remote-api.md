# Remote API

If running the Portal runtime in process is not supported or not desired, values
can be sent over the wire to a remote instance.

The following clients are currently implemented:

- `portal.client.jvm`
  - clj on jvm
  - clj on [babashka](https://babashka.org/)
- `portal.client.node`
  - cljs on [nbb](https://github.com/babashka/nbb)
  - cljs on [lumo](https://github.com/anmonteiro/lumo)
- `portal.client.web`
  - cljs running in the browser
- `portal.client.planck`
  - cljs running in [planck](https://planck-repl.org/)

## Usage

In the process hosting the remote api, do:

``` clojure
(require '[portal.api :as p])
(p/open {:port 5678})
```

In the client process, do:

``` clojure
(require '[portal.client.jvm :as p])
;; (require '[portal.client.node :as p])
;; (require '[portal.client.web :as p])

(def submit (partial p/submit {:port 5678})) ;; :encoding :edn is the default
;; (def submit (partial p/submit {:port 5678 :encoding :json}))
;; (def submit (partial p/submit {:port 5678 :encoding :transit}))

(add-tap #'submit)
```

> [!NOTE]
> `tap>`'d values must be serializable as edn, transit or json.

## Tips

Platform specific tips.

### Planck

Since Planck can load libraries from a jar, you can use the `clj` tool to
generate the class path and pass it in via the `--classpath` cli argument.

To start a Planck with Portal REPL, do:

```bash
clj -Spath -Sdeps '{:deps {djblue/portal {:mvn/version "LATEST"}}}' > .classpath
planck -c `cat .classpath`
```

Then at the REPL, do:

```clojure
(require '[portal.client.planck :as p])
(def submit (partial p/submit {:port 5678}))
(add-tap #'submit)
(tap> :hello-from-planck)
```