(ns user
  (:require [portal.api :as p]))

(defn lazy-fn [symbol]
  (fn [& args] (apply (requiring-resolve symbol) args)))

(def start!       (lazy-fn 'shadow.cljs.devtools.server/start!))
(def watch        (lazy-fn 'shadow.cljs.devtools.api/watch))
(def repl         (lazy-fn 'shadow.cljs.devtools.api/repl))

(defn cljs
  ([] (cljs :client))
  ([build-id] (start!) (watch build-id) (repl build-id)))

(defn node [] (cljs :node))

(comment
  (require '[portal.api :as p])
  (add-tap #'p/submit)

  (start!)
  (watch :pwa)
  (node)

  (p/docs {:mode :dev})

  (def value (atom nil))
  (reset! value {})
  (reset! value {:hello :world})
  (reset! value (map #(-> [::index %]) (range 100)))
  (def portal (p/open))
  (def dev    (p/open {:mode :dev}))
  (def dev    (p/open {:mode :dev :value value}))
  (def emacs  (p/open {:mode :dev :launcher :emacs}))
  (def code   (p/open {:mode :dev :editor :vs-code}))
  (def idea   (p/open {:mode :dev :launcher :intellij}))
  (def work   (p/open {:mode :dev :main 'workspace}))
  (def remote (p/open {:runtime {:type :socket :port 5555}}))
  (def remote (p/open {:runtime {:type :socket :port 6666}}))

  (def app (p/open {:mode :dev ;; for live reload
                    :app  true
                    :port 4006}))

  (p/eval-str "(portal.ui.commands/select-parent portal.ui.state/state)")

  (type (p/eval-str "#js {}"))
  (type (p/eval-str "{}"))
  (type (p/eval-str "(+ 1 2 3)"))

  (add-tap #'p/submit)
  (remove-tap #'p/submit)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (doseq [i (range 100)] (tap> [::index i]))
  (p/clear)
  (p/close)

  (-> @dev)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (require '[portal.console :as log])

  (do (log/trace ::trace)
      (log/debug ::debug)
      (log/info  ::info)
      (log/warn  ::warn)
      (log/error ::error))

  (tap> 4611681620380904123)

  (require '[examples.data :refer [data]])
  (dotimes [_i 25] (tap> data))



  (do
    (require '[examples.data :refer [data]])
    (def app (p/open {:mode :dev ;; for live reload
                      :app  true
                      :port 4007}))
    (add-tap #'p/submit)
    (tap> "hello")

    (tap> (with-meta ["test3"]
            {:portal.viewer/default :portal.viewer/proc-par})
          )

    (tap> (with-meta {:a "test3"
                      :b "ok"}
            {:portal.viewer/default :portal.viewer/proc-par})
          )


    (tap> examples.data/proc-par-simple)

    (tap> examples.data/proc-par-abc)

    )


  )
