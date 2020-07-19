all: release

node_modules: package.json
	npm ci

dev: node_modules
	clojure -A:cider:cljs:dev-cljs:shadow-cljs watch client node demo

release: node_modules
	clojure -A:cljs:shadow-cljs release client

lint:
	clj-kondo --lint src
	clojure -A:cljfmt check

fmt:
	clojure -A:cljfmt fix
