BABASHKA_CLASSPATH := $(shell clojure -A:test -Spath)
PATH  := $(PWD)/target:$(PATH)
ENV   := PATH=$(PATH) BABASHKA_CLASSPATH=$(BABASHKA_CLASSPATH)
SHELL := env $(ENV) /bin/bash

.PHONY: dev test

all: release

clean:
	rm -rf target resources/portal/

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

resources/portal/main.js:
	clojure -M:cljs:shadow-cljs release client

resources/portal/ws.js:
	npx browserify --node \
		--exclude bufferutil \
		--exclude utf-8-validate \
		--standalone Server \
		--outfile resources/portal/ws.js \
		node_modules/ws

dev: node_modules release
	clojure -M:dev:cider:cljs:dev-cljs:shadow-cljs watch client

dev/node: node_modules resources/portal/ws.js release
	clojure -M:dev:cider:cljs:dev-cljs:shadow-cljs watch node client

release: node_modules resources/portal/main.js resources/portal/ws.js

lint/check:
	clojure -M:nrepl:check

lint/kondo:
	clojure -M:kondo --lint dev src test

lint/cljfmt:
	clojure -M:cljfmt check

lint: lint/check lint/kondo lint/cljfmt

target:
	mkdir -p target

test/jvm: release target
	clojure -M:test -m portal.test-runner

test/bb: release bb
	bb -m portal.test-runner

test: test/jvm test/bb

fmt:
	clojure -M:cljfmt fix

pom.xml: deps.edn
	clojure -Spom

install:
	mvn install

deploy: pom.xml
	mvn deploy

ci: lint test

e2e/jvm: release
	@echo "running e2e tests for jvm"
	@clojure -M:test -m portal.e2e | clojure -M -e "(set! *warn-on-reflection* true)" -r

e2e/node: release
	@echo "running e2e tests for node"
	@clojure -M:test -m portal.e2e | clojure -M:cljs -m cljs.main -re node

e2e/bb: release bb
	@echo "running e2e tests for babashka"
	@clojure -M:test -m portal.e2e | bb

e2e/web: release
	@echo "running e2e tests for web"
	@echo "please wait for browser to open before proceeding"
	@clojure -M:test -m portal.e2e web | clojure -M:cljs -m cljs.main

e2e: e2e/jvm e2e/node e2e/web e2e/bb

main/jvm:
	cat deps.edn | clojure -m portal.main edn

main/bb:
	cat deps.edn | bb -cp src:resources -m portal.main edn

demo: bb release
	./build-demo
