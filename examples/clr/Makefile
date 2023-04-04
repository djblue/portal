repl: .classpath
	@CLOJURE_LOAD_PATH=`cat .classpath` Clojure.Main

.classpath: deps.edn
	clojure -X:deps prep
	clojure -Spath > .classpath

deps/nuget:
	nuget install clojure.data.json -Version 2.4.0

deps/clj: .classpath

clean:
	rm .classpath
