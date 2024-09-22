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
  (def work   (p/open {:mode :boot :main 'workspace/app}))
  (def tetris (p/open {:mode :boot :main 'tetris.core/app}))

  (def code   (p/open {:mode :dev #_#_:launcher :vs-code}))
  (def boot   (p/open {:mode :boot #_#_:launcher :vs-code}))

  (p/repl portal)

  (require '[examples.data :refer [data]])
  (dotimes [_i 25] (tap> data)))

;; bootstrap issues
;; - automate bootstrap setup
;;   - make sure not to break joyride/nbb with this change
;; - [x] (require 'my.ns :reload) will load code from cache again bypassing changes to file
;;   - [x] Could remove file from cache on first load resulting in subsequent loads using /load?
;; - load-file needs to go from {:context :expr} to {:context :statement}
;; - can multiple caches be composed?
;;   - what can the UI do to help manage the boot cache?
;;   - can a boot cache as a sort of client-size jar?
;;   - what get's resolved on collisions?
;; - How can we avoid duplicate deps in "resources/portal-boot/main.js" and boot caches?
;;   - What is even currently being loaded from main.js vs boot-cache.json
;; - need to setup tests against {:mode :boot}
;;   - add more tests for eval'ing different types of forms, currently very limited
;; - code cleanup
;; - [x] extract node.js changes into their own commit
;; - [x] clojure.datafy warning in console
;; - [] automatically build boot cache during build
;; - setup stdout/stderr for repl
;; - add cljs stack trace parsing
;;   - use this stack trace when throwing on backend
;; - [x] invalidate cache entry when file changes
;; - render/load-file indictor kinda like shadow-cljs

;; bootstrap issues done
;; - [x] figure out with clojure.pprint and clojure.datafy are causing issues
;;   - eval-string wasn't using the same bootstrap state
;; - [x] portal.resources/inline needs to use /load to load icons
;; - [x] main sure tetris demo still works
;;   - works!
;; - [x] dev setup via portal.setup preload
;;    - got this working but not automatically setup
;; - [x] re-render app of file-load, mimics file save reloading
;; - [x] loading from bootstrap cljs cache breaks sci repl integration
;;    - need to bypass cache when sci

;; How to do caching?
;; Constraints
;; - fast-ish to load
;;   - quick to batch load
;; - correctly invalidates when source is modified