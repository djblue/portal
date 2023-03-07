(ns ^:no-doc portal.ui.cljs)

(def eval-fn (atom nil))

(defn eval-string [input] (@eval-fn input))

(def init-fn (atom nil))

(defn init [] (when-let [f @init-fn] (f)))
