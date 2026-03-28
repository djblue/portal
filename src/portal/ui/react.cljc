(ns ^:no-doc portal.ui.react
  #?(:clj  (:refer-clojure :exclude [random-uuid]))
  #?(:cljs (:require ["react" :as react])
     :clj  (:require [portal.runtime.polyfill :refer [random-uuid]]
                     [portal.runtime.react :as react]))
  #?(:cljs (:require-macros portal.ui.react)))

(def ^:dynamic *static* false)

(defn use-effect* [f deps]
  (when-not *static*
    (case deps
      :always #?(:clj  (react/use-effect f)
                 :cljs (react/useEffect f))
      :once #?(:clj  (react/use-effect f [])
               :cljs (react/useEffect f #js []))
      #?(:clj  (react/use-effect f deps)
         :cljs (react/useEffect f (to-array deps))))))

(def no-unmount #?(:clj nil :cljs js/undefined))

(defmacro use-effect [deps & body]
  `(when-not *static*
     (use-effect*
      (fn []
        (let [result# (do ~@body)]
          (if (fn? result#) result# no-unmount)))
      ~deps)))

(defn use-state [initial-value]
  (if *static*
    #?(:clj      [initial-value (constantly nil)]
       :cljs #js [initial-value (constantly nil)])
    #?(:clj  (react/use-state initial-value)
       :cljs (react/useState initial-value))))

(defn create-context [default-value]
  #?(:clj  (react/create-context default-value)
     :cljs (react/createContext default-value)))

(defn use-context [context]
  (if *static*
    #?(:clj (:default-value context)    :cljs (.-_currentValue context))
    #?(:clj (react/use-context context) :cljs (react/useContext context))))

(defn provider [context value & children]
  #?(:clj  (apply react/provider context value children)
     :cljs (into [:r> (.-Provider ^js context) #js {:value value}] children)))

#?(:cljs
   (defn use-ref
     ([]
      (use-ref nil))
     ([initial-value]
      (if *static*
        #js {:current js/undefined}
        (react/useRef initial-value)))))

(defn use-memo* [f deps]
  (if *static*
    (f)
    (case deps
      :once #?(:clj (react/use-memo f []) :cljs (react/useMemo f #js []))
      #?(:clj (react/use-memo f deps) :cljs (react/useMemo f deps)))))

(defmacro use-memo [deps & body]
  `(use-memo* (fn [] ~@body) ~deps))

(defn use-atom
  ([a]
   (use-atom a identity))
  ([a f]
   (use-atom a f true))
  ([a f active?]
   (let [[state set-state!] (use-state (f @a))]
     (use-effect*
      (fn []
        (when active?
          (let [id (random-uuid)
                cache (atom state)
                watcher
                (fn watcher [_ _ state state']
                  (when-not (= state state')
                    (let [value (f state')]
                      (when-not (= @cache value)
                        (reset! cache value)
                        (set-state! (cond-> value (fn? value) constantly))))))]
            (watcher nil nil state @a)
            (add-watch a id watcher)
            (fn []
              (remove-watch a id)))))
      [a active?])
     state)))