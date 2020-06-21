# portal

## development

    clojure -A:shadow-cljs watch app

    :CljEval (shadow/repl :app)

## cli

    cat deps.edn | clj -A:edn

    cat package.json | clj -A:json
