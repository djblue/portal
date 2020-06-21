all: release

node_modules: package.json
	npm ci

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch app

release:
	clojure -A:cljs:shadow-cljs release app

lint:
	clj-kondo --lint src
