export BABASHKA_CLASSPATH := $(shell clojure -A:test -Spath)
export PATH := $(PWD)/target:$(PATH)
VERSION := 0.6.4

.PHONY: dev test

all: dev

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

resources/portal/main.js: node_modules
	clojure -M:cljs:shadow-cljs release client

resources/portal/ws.js: node_modules
	npx browserify --node \
		--exclude bufferutil \
		--exclude utf-8-validate \
		--standalone Server \
		--outfile resources/portal/ws.js \
		node_modules/ws

resources/js: resources/portal/main.js resources/portal/ws.js

dev: resources/js
	clojure -M:dev:cider:cljs:dev-cljs:shadow-cljs watch client

dev/node: resources/js
	clojure -M:dev:cider:cljs:dev-cljs:shadow-cljs watch node client

check/clj-check:
	clojure -M:nrepl:check

check/clj-kondo:
	clojure -M:kondo --lint dev src test

check/cljfmt:
	clojure -M:cljfmt check

check/npm-deps:
	npm outdated

check/clj-deps:
	clojure -M:antq

check/deps: check/clj-deps check/npm-deps

lint: check/clj-check check/clj-kondo check/cljfmt

target:
	mkdir -p target

test/jvm: resources/js target
	clojure -M:test -m portal.test-runner

test/bb: resources/js bb
	bb -m portal.test-runner

test: test/jvm test/bb

fmt:
	clojure -M:cljfmt fix

ci: lint test

e2e/jvm: resources/js
	@echo "running e2e tests for jvm"
	@clojure -M:test -m portal.e2e | clojure -M -e "(set! *warn-on-reflection* true)" -r

e2e/node: resources/js
	@echo "running e2e tests for node"
	@clojure -M:test -m portal.e2e | clojure -M:cljs -m cljs.main -re node

e2e/bb: resources/js bb
	@echo "running e2e tests for babashka"
	@clojure -M:test -m portal.e2e | bb

e2e/web: resources/js
	@echo "running e2e tests for web"
	@echo "please wait for browser to open before proceeding"
	@clojure -M:test -m portal.e2e web | clojure -M:cljs -m cljs.main

e2e: e2e/jvm e2e/node e2e/web e2e/bb

main/jvm:
	cat deps.edn | clojure -m portal.main edn

main/bb:
	cat deps.edn | bb -cp src:resources -m portal.main edn

demo: resources/js bb
	./build-demo

set-version:
	bb -cp dev -m version ${VERSION}
	git add .
	git commit -m "Release ${VERSION}"

pom.xml: deps.edn
	clojure -Spom

jar: pom.xml resources/js
	mvn package

install: jar
	mvn install

release: set-version clean ci jar

deploy: release
	mvn deploy
