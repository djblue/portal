(ns portal.ui.boot
  (:require [cljs.js :as cljs]
            [cognitect.transit]))

(def state (cljs/empty-state))

(defn load-fn [_ done]
  #_(prn m)
  (done {:lang :js :source ""}))

(def ^:dynamic js-eval nil)

(defn eval-str
  ([source]
   (eval-str source {}))
  ([source opts]
   (js/Promise.
    (fn [resolve reject]
      (cljs/eval-str
       state
       source
       (:name opts "<REPL>")
       {:eval (fn [{:keys [source name] :as _resource}]
                (js-eval source name))
        :def-emits-var (:def-emits-var opts true)
        :target :nodejs
        :source-map true
        :context (:context opts :statement)
        :verbose (:verbose opts false)
        :load (:load opts load-fn)
        :ns (:ns opts 'cljs.user)
        :cache-source (:cache-source opts)}
       (fn [{:keys [error] :as result}]
         #_(when error (prn error))
         (when (and (:verbose opts) error) (prn error))
         (if error
           (reject result)
           (resolve result))))))))

(when (exists? js/window)
  (set! (.-eval_cljs js/window) eval-str))

(when (exists? js/module)
  (set! (.-user js/cljs) #js {})
  (set! (.-exports js/module) #js {:evalStr eval-str
                                   :cljs js/cljs
                                   :global goog/global
                                   :goog js/goog}))