(ns ^:no-doc portal.ui.react
  #?(:cljs (:require ["react" :as react]))
  #?(:cljs (:require-macros portal.ui.react)))

(def ^:dynamic *static* false)

#?(:cljs
   (defn use-effect* [f deps]
     (when-not *static*
       (case deps
         :always (react/useEffect f)
         :once (react/useEffect f #js [])
         (react/useEffect f deps)))))

(defmacro use-effect [deps & body]
  `(when-not *static*
     (use-effect*
      (fn []
        (let [result# (do ~@body)]
          (if (fn? result#) result# js/undefined)))
      ~deps)))

#?(:cljs
   (defn use-state [initial-value]
     (if *static*
       #js [initial-value (constantly nil)]
       (react/useState initial-value))))

#?(:cljs
   (defn create-context [default-value]
     (react/createContext default-value)))

#?(:cljs
   (defn use-context  [context]
     (if *static*
       (.-_currentValue context)
       (react/useContext context))))

#?(:cljs
   (defn use-ref
     ([]
      (use-ref nil))
     ([initial-value]
      (if *static*
        #js {:current js/undefined}
        (react/useRef initial-value)))))

#?(:cljs
   (defn use-memo* [f deps]
     (if *static*
       (f)
       (case deps
         :always (react/useMemo f)
         :once (react/useMemo f #js [])
         (react/useMemo f deps)))))

(defmacro use-memo [deps & body]
  `(use-memo* (fn [] ~@body) ~deps))