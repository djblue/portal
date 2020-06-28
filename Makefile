all: release

node_modules: package.json
	npm ci

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch browser node

release:
	clojure -A:cljs:shadow-cljs release browser

lint:
	clj-kondo --lint src
