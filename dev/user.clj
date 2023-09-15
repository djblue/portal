(ns user)

(defn lazy-fn [symbol]
  (fn [& args] (apply (requiring-resolve symbol) args)))

(def start! (lazy-fn 'shadow.cljs.devtools.server/start!))
(def watch  (lazy-fn 'shadow.cljs.devtools.api/watch))
(def repl   (lazy-fn 'shadow.cljs.devtools.api/repl))

(defn cljs
  ([] (cljs :client))
  ([build-id] (start!) (watch build-id) (repl build-id)))

(defn node [] (cljs :node))

(comment
  (require '[portal.api :as p])
  (add-tap #'p/submit)
  (remove-tap #'p/submit)

  (watch :pwa)

  (p/clear)
  (p/close)
  (p/docs {:mode :dev})

  (def portal (p/open))
  (def dev    (p/open {:mode :dev}))
  (def emacs  (p/open {:mode :dev :launcher :emacs}))
  (def code   (p/open {:mode :dev :launcher :vs-code}))
  (def idea   (p/open {:mode :dev :launcher :intellij}))
  (def work   (p/open {:mode :dev :main 'workspace/app}))
  (def tetris (p/open {:mode :dev :main 'tetris.core/app}))

  (p/repl portal)

  (require '[examples.data :refer [data]])
  (dotimes [_i 25] (tap> data)))