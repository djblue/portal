repl: .classpath
	@CLOJURE_LOAD_PATH=`cat .classpath` Clojure.Main

.classpath: deps.edn
	clojure -X:deps prep
	clojure -Spath > .classpath

clean:
	rm .classpath
