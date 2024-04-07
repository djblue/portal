(ns ^:no-doc portal.ui.lazy)

(defmacro use-lazy [k value] `(use-lazy* ~k (fn [] ~value)))
