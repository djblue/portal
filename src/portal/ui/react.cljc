(ns ^:no-doc portal.ui.react
  #?(:cljs (:require ["react" :as react]))
  #?(:cljs (:require-macros portal.ui.react)))

#?(:cljs
   (defn use-effect* [f deps]
     (case deps
       :always (react/useEffect f)
       :once (react/useEffect f #js [])
       (react/useEffect f deps))))

(defmacro use-effect [deps & body]
  `(use-effect*
    (fn []
      (let [result# (do ~@body)]
        (if (fn? result#) result# js/undefined)))
    ~deps))