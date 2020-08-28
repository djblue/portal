BABASHKA_CLASSPATH := $(shell clojure -A:test -Spath)
PATH  := $(PWD)/target:$(PATH)
ENV   := PATH=$(PATH) BABASHKA_CLASSPATH=$(BABASHKA_CLASSPATH)
SHELL := env $(ENV) /bin/bash

.PHONY: dev test

all: release

clean:
	rm -rf target resources/main.js

target/install-babashka:
	mkdir -p target
	curl -s https://raw.githubusercontent.com/borkdude/babashka/master/install -o target/install-babashka
	chmod +x target/install-babashka

target/bb: target/install-babashka
	target/install-babashka $(PWD)/target
	touch target/bb

bb: target/bb

node_modules: package.json
	npm ci

resources/main.js:
	clojure -A:cljs:shadow-cljs release client

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch client demo

release: node_modules resources/main.js

lint:
	clojure -A:nrepl:check
	clojure -A:kondo --lint dev src test
	clojure -A:cljfmt check

test/jvm:
	clojure -A:test -m portal.test-runner

test/bb: bb
	bb -m portal.test-runner

test: test/jvm test/bb

fmt:
	clojure -A:cljfmt fix

pom.xml: deps.edn
	clojure -Spom

install:
	mvn install

deploy: pom.xml
	mvn deploy

ci: lint test

e2e/jvm: release
	@echo "running e2e tests for jvm"
	@clojure -m portal.e2e | clojure -e "(set! *warn-on-reflection* true)" -r

e2e/node: release
	@echo "running e2e tests for node"
	@clojure -m portal.e2e | clojure -A:cljs -m cljs.main -re node

e2e/bb: release bb
	@echo "running e2e tests for babashka"
	@clojure -m portal.e2e | bb

e2e/web: release
	@echo "running e2e tests for web"
	@echo "please wait for browser to open before proceeding"
	@clojure -m portal.e2e web | clojure -A:cljs -m cljs.main

e2e: e2e/jvm e2e/node e2e/web e2e/bb
