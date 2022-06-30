((nil . ((cider-clojure-cli-global-options     . "-A:dev:cljs:shadow")
         (cider-custom-cljs-repl-init-form     . "(user/cljs)")
         (cider-default-cljs-repl              . custom)
         (cider-preferred-build-tool           . clojure-cli)
         (cider-redirect-server-output-to-repl . t)
         (cider-repl-display-help-banner       . nil)
         (clojure-toplevel-inside-comment-form . t)
         (eval . (progn
                   (make-variable-buffer-local 'cider-jack-in-nrepl-middlewares)
                   (add-to-list 'cider-jack-in-nrepl-middlewares
                                "cider.nrepl/cider-middleware")
                   (add-to-list 'cider-jack-in-nrepl-middlewares
                                "portal.nrepl/wrap-portal")
                   (add-to-list 'cider-jack-in-nrepl-middlewares
                                "shadow.cljs.devtools.server.nrepl/middleware"))))))
