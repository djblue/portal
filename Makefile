all: release

node_modules: package.json
	npm ci

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch client node demo

release: node_modules
	clojure -A:cljs:shadow-cljs release client

lint:
	clojure -A:kondo --lint src
	clojure -A:cljfmt check

fmt:
	clojure -A:cljfmt fix

pom.xml: deps.edn
	clojure -Spom

install:
	mvn install

deploy: pom.xml
	mvn deploy

ci: lint
