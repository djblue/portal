(ns portal.ui.load)

(defn closure-wrap [{:keys [source file dir]}]
  (str
   "(function () {"
   "var __filename = " (pr-str file) ";\n"
   "var __dirname  = " (pr-str dir) ";\n"
   "var exports    = {};\n"
   "var module     = {};\n"
   "var require    = window.require_parent(" (pr-str file) ");\n"
   source " ;\n"
   "return module.exports || exports;\n"
   "})()"))

(defn load-fn-sync [m]
  (let [_label (str "load-fn-sync: " (pr-str m))
        xhr  (js/XMLHttpRequest.)]
    #_(.time js/console _label)
    (.open xhr "POST" "/load" false)
    (.setRequestHeader xhr "content-type" "application/edn")
    (.send xhr (pr-str m))
    #_(.timeEnd js/console _label)
    (some->
     (.parse js/JSON (.-responseText xhr))
     (js->clj :keywordize-keys true)
     (update :lang keyword))))

(def load-cache (atom {}))
(def require-cache (atom {}))

(defn- node-require
  ([module]
   (node-require nil module))
  ([parent module]
   (get
    (swap! require-cache
           (fn [cache]
             (if (contains? cache module)
               cache
               (let [k     (str parent "$" module)
                     value (get
                            (swap!
                             load-cache
                             (fn [cache]
                               (if (contains? cache k)
                                 cache
                                 (assoc cache k (load-fn-sync {:npm true :name module :parent parent})))))
                            k)]
                 (assoc cache module (js/eval (closure-wrap value)))))))
    module)))

(defn- node-require-with-parent [parent]
  (fn [module] (node-require parent module)))

(set! (.-require js/window) node-require)
(set! (.-require_parent js/window) node-require-with-parent)
(set! (.-process js/window)
      #js {:env #js {:NODE_ENV
                     (if js/goog.DEBUG
                       "development"
                       "production")}})
