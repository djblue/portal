.PHONY: dev

all: release

node_modules: package.json
	npm ci

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch client node demo

release: node_modules
	clojure -A:cljs:shadow-cljs release client

lint:
	clojure -A:kondo --lint dev src test
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

e2e/jvm:
	@echo "running e2e tests for jvm"
	@clojure -m portal.e2e | clojure

e2e/node:
	@echo "running e2e tests for node"
	@clojure -m portal.e2e | clojure -A:cljs -m cljs.main -re node

e2e/web:
	@echo "running e2e tests for web"
	@echo "please wait for browser to open before proceeding"
	@clojure -m portal.e2e web | clojure -A:cljs -m cljs.main

e2e: e2e/jvm e2e/node e2e/web
