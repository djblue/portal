# CLI Usage

Add a portal alias in `~/.clojure/deps.edn`

```clojure
:portal/cli
{:main-opts ["-m" "portal.main"]
 :extra-deps
 {djblue/portal {:mvn/version "LATEST"}
  ;; optional yaml support
  clj-commons/clj-yaml {:mvn/version "0.7.0"}}}
```

Then do the following depending on your data format:

```bash
cat data | clojure -M:portal/cli [edn|json|transit|yaml]
# or with babashka for faster startup
cat data | bb -cp `clojure -Spath -M:portal/cli` -m portal.main [edn|json|transit|yaml]
```

I keep the following bash aliases handy for easier CLI use:

```bash
alias portal='bb -cp `clojure -Spath -M:portal/cli` -m portal.main'
alias edn='portal edn'
alias json='portal json'
alias transit='portal transit'
alias yaml='portal yaml'
```

and often use the `Copy as cURL` feature in the chrome network tab to do
the following:

```bash
curl ... | transit
```

There is also the ability to invoke a standalone http server to listen and
display data from remote client

```bash
bb -cp `clojure -Spath -Sdeps '{:deps {djblue/portal {:mvn/version "LATEST"}}}'` \
   -e '(require (quote [portal.api])) (portal.api/open {:port 53755}) @(promise)'
```
