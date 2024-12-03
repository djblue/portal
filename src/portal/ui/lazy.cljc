(ns portal.ui.lazy
  {:no-doc true})

(defmacro use-lazy [k value] `(use-lazy* ~k (fn [] ~value)))
