(ns ^:no-doc portal.ui.load
  (:require [cognitect.transit :as transit]))

(defn- module-wrapper
  "https://nodejs.org/api/modules.html#the-module-wrapper"
  [{:keys [source]}]
  (str "(function (exports, require, module, __filename, __dirname) { " source "\n });"))

(def conn (atom nil))

(defn- ->host [path]
  (if-let [{:keys [host port]} @conn]
    (str "http://" host ":" port path)
    (if (exists? js/PORTAL_HOST)
      (str "http://" js/PORTAL_HOST path)
      path)))

(def cache (atom nil))

(def preload-requests (atom #{}))

(defn npm? [data]
  (or (= 'react (:name data))
      (= 'react-dom (:name data))
      (string? (:name data))
      (:npm data)))

(defn add-cache! [m value]
  (swap! cache assoc m value)
  value)

(defn read-body-xhr [^js/XMLHttpRequest xhr]
  (let [content-type (.getResponseHeader xhr "content-type")]
    (case content-type
      "application/transit+json"
      (transit/read (transit/reader :json) (.-responseText xhr))
      "application/json"
      (js->clj (.parse js/JSON (.-responseText xhr)) :keywordize-keys true))))

(defn read-body-fetch [^js/Response response]
  (let [content-type (.get (.-headers response) "content-type")]
    (case content-type
      "application/transit+json"
      (-> (.text response)
          (.then #(transit/read (transit/reader :json) %)))
      "application/json"
      (-> (.json response)
          (.then #(some->
                   %
                   (js->clj :keywordize-keys true)
                   (update :lang keyword)))))))

(defn- normalize-key [cache-key]
  (str "key=" (.replaceAll cache-key "/" ".") ".json"))

(defn load-fn-sync [m]
  (swap! preload-requests conj m)
  (if-let [hit (get @cache m)]
    (do (swap! cache dissoc m) hit)
    (let [_label (str "load-fn-sync: " (pr-str m))
          xhr  (js/XMLHttpRequest.)]
      #_(.time js/console _label)
      (.open xhr "POST" (->host (str "/load?" (:path m))) false)
      (.setRequestHeader xhr "content-type" "application/edn")
      (.send xhr (pr-str m))
      #_(.timeEnd js/console _label)
      (let [out (some->
                 (read-body-xhr xhr)
                 (update :lang keyword)
                 (assoc :name (:name m)))]
        (add-cache! m out)
        out))))

(def ^:private require-cache (atom {}))

(defn- relative? [module-name] (.startsWith (name module-name) "."))

(defn get-module
  ([module-name]
   (get-module nil module-name))
  ([parent module-name]
   (let [k (if (relative? module-name) [parent module-name] module-name)]
     (or (some-> ^Module (get @require-cache module-name) .-exports)
         (some-> ^Module (get @require-cache k) .-exports)))))

(defn cache-put
  ([]
   (cache-put "boot" @preload-requests))
  ([cache-key cache-value]
   (js/fetch (->host (str "/cache?" (normalize-key cache-key)))
             #js{:method "POST"
                 :body (transit/write (transit/writer :json) cache-value)})))

(defn cache-get
  ([]
   (cache-get "boot"))
  ([cache-key]
   (if-let [cache (not-empty @cache)]
     (.resolve js/Promise cache)
     (-> (js/fetch (->host (str "/cache?" (normalize-key cache-key)))
                   #js {:method "GET"})
         (.then read-body-fetch)))))

(defn load-cache []
  (if-let [cache @cache]
    (.resolve js/Promise cache)
    (-> (cache-get)
        (.then #(do (reset! cache %)))
        (.catch #(do (reset! cache {}))))))

(set! (.-saveCache js/window) cache-put)
(set! (.-loadCache js/window) load-cache)

(defn- load-fn* [{:keys [macros] :as m}]
  (swap! preload-requests conj m)
  (-> (load-cache)
      (.then
       (fn []
         (let [hit (get @cache m)]
           (if hit
             (do (swap! cache dissoc m) hit)
             (if (and (npm? m) (get-module (name (:name m))))
               (.resolve js/Promise nil)
               (-> (js/fetch (->host (str "/load?" (normalize-key (str (:path m) (when macros "$macros")))))
                             #js{:method "POST"
                                 :headers #js {"content-type" "application/edn"}
                                 :body (pr-str m)})
                   (.then read-body-fetch)
                   (.then #(add-cache! m %))))))))))

(def preloaded? #{'cljs.js
                  'cljs.core
                  'cljs.compiler
                  'cljs.analyzer.impl
                  'cljs.source-map
                  'cljs.env.macros})

(def fast-fail? #{'clojure.core
                  'clojure.pprint
                  'clojure.spec.alpha
                  'clojure.test})

(def load-time (atom 0))

(defn load-fn [{:keys [name path] :as data} done]
  (let [start (.now js/Date)]
    (cond
      (fast-fail? name)
      (done nil)

      (or (preloaded? name)
          (.startsWith path "goog")
          (npm? data))
      (done {:lang :js :source ""})

      :else
      (.then (load-fn* data)
             (fn [data]
               (swap! load-time + (- (.now js/Date) start))
               (done data))))))

(defn cache-source [m done]
  (done {:value nil})
  (let [macros (when (.includes (str (:name (:cache m))) "$macros")
                 true)
        file (str (:path m) (when macros "$macros"))
        m (assoc m :lang :js :file file)
        k (-> (select-keys m [:name :path])
              (assoc :macros macros))]
    (add-cache! k m)
    (cache-put file m)))

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
   (let [k (if (relative? module-name) [parent module-name] module-name)]
     (or
      (get-module parent module-name)
      (try
        (let [{:keys [file] :as value} (load-fn-sync {:npm true :name module-name :parent parent})]
          (if-let [^Module module (get @require-cache file)]
            (.-exports module)
            (let [exports    #js {}
                  module-obj (Module. exports)]
              (swap! require-cache assoc file module-obj k module-obj)
              ((js/eval (module-wrapper value))
               exports #(node-require (:dir value) %) module-obj (:file value) (:dir value))
              (.-exports module-obj))))
        (catch :default e
          (.error js/console e)
          (throw e)))))))

(def require-time (atom 0))

(set! (.-require js/window)
      (fn [& args]
        (let [start (.now js/Date)
              out (apply node-require args)]
          (swap! require-time + (- (.now js/Date) start))
          out)))
(set! (.-process js/window)
      #js {:env #js {:NODE_ENV
                     "production"
                     #_(if js/goog.DEBUG
                         "development"
                         "production")}})
