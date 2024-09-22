(ns ^:no-doc portal.ui.cljs)

(def eval-fn (atom nil))

(defn eval-string [input]
  (let [f @eval-fn]
    (if (fn? f)
      (f input)
      (throw (ex-info "No eval-fn setup." {:input input})))))

(def init-fn (atom nil))

(defn init [] (when-let [f @init-fn] (f)))
