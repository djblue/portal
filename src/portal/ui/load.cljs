(ns ^:no-doc portal.ui.load)

(defn- module-wrapper
  "https://nodejs.org/api/modules.html#the-module-wrapper"
  [{:keys [source]}]
  (str "(function (exports, require, module, __filename, __dirname) { " source "\n });"))

(def conn (atom nil))

(defn load-fn-sync [m]
  (let [_label (str "load-fn-sync: " (pr-str m))
        xhr  (js/XMLHttpRequest.)]
    #_(.time js/console _label)
    (.open xhr "POST"
           (if-let [{:keys [host port]} @conn]
             (str "http://" host ":" port "/load")
             (if (exists? js/PORTAL_HOST)
               (str "http://" js/PORTAL_HOST "/load")
               "/load"))
           false)
    (.setRequestHeader xhr "content-type" "application/edn")
    (.send xhr (pr-str m))
    #_(.timeEnd js/console _label)
    (some->
     (.parse js/JSON (.-responseText xhr))
     (js->clj :keywordize-keys true)
     (update :lang keyword)
     (assoc :name (:name m)))))

(def ^:private require-cache (atom {}))

(deftype Module [exports])

(defn load-require-cache [modules]
  (swap!
   require-cache
   (fn [cache]
     (reduce-kv
      (fn [cache module-name export]
        (assoc cache module-name (Module. export)))
      cache
      modules))))

(defn node-require
  ([module]
   (node-require nil module))
  ([parent module-name]
   (or
    (some-> ^Module (get @require-cache module-name) .-exports)
    (try
      (let [{:keys [file] :as value} (load-fn-sync {:npm true :name module-name :parent parent})]
        (if-let [^Module module (get @require-cache file)]
          (.-exports module)
          (let [exports    #js {}
                module-obj (Module. exports)]
            (swap! require-cache assoc file module-obj)
            ((js/eval (module-wrapper value))
             exports #(node-require (:dir value) %) module-obj (:file value) (:dir value))
            (.-exports module-obj))))
      (catch :default e
        (.error js/console e)
        (throw e))))))

(set! (.-require js/window) node-require)
(set! (.-process js/window)
      #js {:env #js {:NODE_ENV
                     (if js/goog.DEBUG
                       "development"
                       "production")}})
