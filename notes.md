use cider-jack-in command


clojure -A:dev:cljs:shadow -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.0.0"} cider/cider-nrepl {:mvn/version "0.28.7"} refactor-nrepl/refactor-nrepl {:mvn/version "3.6.0"}} :aliases {:cider/nrepl {:main-opts ["-m" "nrepl.cmdline" "--middleware" "[shadow.cljs.devtools.server.nrepl/middleware,portal.nrepl/wrap-portal,refactor-nrepl.middleware/wrap-refactor,cider.nrepl/cider-middleware]"]}}}' -M:cider/nrepl


Then connect with emacs


In another bash term:

clojure -M:cljs:shadow -m shadow.cljs.devtools.cli watch pwa

Shadow UI
http://localhost:9630


make changes to the CLJS files. Ex. log.cljs



```bb dev
```

calls
```clojure -M:cljs:shadow -m shadow.cljs.devtools.cli release client
```


This seems to work ONCE - no reload at all
-> main.js doesn't change? the `release client step is KO?`


To install locally

ls $HOME/.m2/repository/nha/portal/
rm -r .shadow-cljs/ target/

rm -rf $HOME/.m2/repository/nha/portal/ .shadow-cljs/ target/ && clojure -M:cljs:shadow -m shadow.cljs.devtools.cli release client && bb jar && clj -X:deps mvn-install :pom '"target/classes/META-INF/maven/nha/portal/pom.xml"' :jar '"target/portal-0.35.1.jar"'



<!-- bb tag -->
<!-- bb deploy -->


Deploy alternative version to clojars
TODO snapshot

once

bb check
bb e2e
bb ci


```bb deploy````
