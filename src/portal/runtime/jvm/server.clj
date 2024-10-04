(ns ^:no-doc portal.runtime.jvm.server
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [org.httpkit.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.cson :as cson]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.json :as json]
            [portal.runtime.npm :as npm]
            [portal.runtime.rpc :as rpc])
  (:import [java.io File PushbackReader ByteArrayOutputStream]
           [java.util UUID]))

(def ^:private enable-cors
  {:status 204
   :headers
   {"Access-Control-Allow-Origin"  "*"
    "Access-Control-Allow-Headers" "origin, content-type"
    "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
    "Access-Control-Max-Age"       86400}})

(defmulti route (juxt :request-method :uri))

(defn- open-debug [{:keys [options] :as session}]
  (try
    (when (= :server (:debug options))
      ((requiring-resolve 'portal.runtime.debug/open) session))
    (catch Exception e (tap> e) nil)))

(defn- close-debug [instance]
  (try
    (when instance
      ((requiring-resolve 'portal.runtime.debug/close) instance))
    (catch Exception e (tap> e) nil)))

(defmethod route [:get "/rpc"] [request]
  (let [session (rt/open-session (:session request))
        debug   (open-debug session)]
    (server/as-channel
     request
     {:on-receive (fn [_ch message] (rpc/on-receive session message))
      :on-open    (fn [ch] (rpc/on-open session #(server/send! ch %)))
      :on-close   (fn [_ch _status]
                    (close-debug debug)
                    (rpc/on-close session))})))

(defn- send-resource [content-type resource]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    resource})

(defmethod route [:get "/wait.js"] [_]
  (try (Thread/sleep 60000)
       (catch Exception _e {:status 200})))

(defmethod route :default [request]
  (if-not (str/ends-with? (:uri request) ".map")
    {:status 404}
    (let [uri (subs (:uri request) 1)]
      (some
       (fn [^File file]
         (when (and file (.exists file))
           (send-resource "application/json" (slurp file))))
       [(io/file (io/resource (str "portal-dev/" uri)))
        (io/file (io/resource uri))]))))

(defmethod route [:get "/icon.svg"] [_]
  {:status  200
   :headers {"Content-Type" "image/svg+xml"}
   :body (slurp (io/resource "portal/icon.svg"))})

(defmethod route [:get "/main.js"] [request]
  {:status  200
   :headers {"Content-Type" "text/javascript"}
   :body
   (case (-> request :session :options :mode)
     :dev (slurp (io/resource "portal-dev/main.js"))
     :boot (str "window.require = function () {};
                   window.process = {};
                   // global = { btoa: x => btoa(x), require: require, process: process };
                   global = window;
                   "
                (-> (io/resource "portal-boot/main.js")
                    (fs/slurp)
                    (str/replace "#!/usr/bin/env node\n" ""))
                "\nportal.ui.boot.js_eval = (source, file) => {eval(source)}\n"
                "eval_cljs(" (pr-str (fs/slurp "src/portal/ui/load.cljs")) ")\n"
                "eval_cljs(" (pr-str (fs/slurp "src/portal/ui/init.cljs")) ")\n")
     (slurp (io/resource "portal/main.js")))})

(defn- get-session-id [request]
  ;; There might be a referrer which is not a UUID in standalone mode.
  (try
    (some->
     (or (:query-string request)
         (when-let [referer (get-in request [:headers "referer"])]
           (last (str/split referer #"\?"))))
     UUID/fromString)
    (catch Exception _ nil)))

(defn- with-session [request]
  (if-let [session-id (get-session-id request)]
    (assoc request :session (rt/get-session session-id))
    request))

(defn- content-type [request]
  (some-> (get-in request [:headers "content-type"])
          (str/split #";")
          first))

(defn- body [{:keys [body] :as request}]
  (case (content-type request)
    "application/transit+json" (transit/read (transit/reader body :json))
    "application/json"         (json/read-stream (io/reader body))
    "application/cson"         (cson/read (slurp body))
    "application/edn"          (edn/read
                                {:default tagged-literal}
                                (PushbackReader. (io/reader body)))))

(defn- ->js [file]
  (let [source (fs/slurp file)]
    {:lang :js :npm true :file file :dir (fs/dirname file) :source source}))

(defn- node-resolve [{:keys [name parent]}]
  (if-not parent
    (some-> name npm/node-resolve ->js)
    (some-> name (npm/node-resolve parent) ->js)))

(def clj-macros? #{'cljs.compiler.macros
                   'cljs.env.macros
                   'cljs.js
                   'cljs.reader
                   'cljs.tools.reader.reader-types
                   'reagent.core
                   'reagent.debug
                   'reagent.interop
                   'reagent.ratom})

(defn- ->params [request]
  (when-let [query-string (not-empty (:query-string request))]
    (reduce
     (fn [out param]
       (let [[k v] (str/split param #"=")]
         (assoc out (keyword k) v)))
     {}
     (str/split query-string #"&"))))

(defn- ->cache-key [request]
  (when-let [cache-key (:key (->params request))]
    (fs/join ".portal/cache" cache-key)))

(defmethod route [:options "/cache"] [_] enable-cors)

(defn- cache-get [request]
  (let [path (->cache-key request)]
    (when (fs/exists path)
      {:status 200
       :headers (merge
                 {"content-type" "application/transit+json"}
                 (:headers enable-cors))
       :body (fs/slurp path)})))

(defn- ->cache-path [{:keys [path macros]}]
  (when path
    (fs/join ".portal/cache"
             (str (str/replace path "/" ".")
                  (when macros "$macros")
                  ".json"))))

(defn load-fn [m]
  (let [{:keys [name path macros resource use-cache]} m]
    (cond
      resource
      {:source (slurp (io/resource path))}

      (or (= name 'react) (string? name) (:npm name))
      (node-resolve m)

      :else
      (let [cache-path  (->cache-path m)
            source-info (some
                         (fn [ext]
                           (when-let [resource (io/resource (str path ext))]
                             {:lang (if (= ext ".js") :js :clj)
                              :file (str/replace (str resource) #"file:" "")
                              :source (slurp resource)}))
                         (if macros
                           (cond-> [".cljc"]
                             (clj-macros? name)
                             (conj ".clj"))
                           [".cljs" ".cljc" ".js"]))
            cache-modified  (some-> cache-path fs/modified)
            source-modified (some-> source-info :file fs/modified)]
        (if (and use-cache (fs/exists cache-path) (> cache-modified source-modified))
          (transit/read (transit/reader (io/input-stream cache-path) :json))
          source-info)))))

(defmethod route [:get "/cache"] [request]
  (if (= "boot.json" (:key (->params request)))
    {:status 200
     :headers {"content-type" "application/transit+json"}
     :body
     (let [out (ByteArrayOutputStream. 1024)]
       (transit/write
        (transit/writer out :json {:transform transit/write-meta})
        (reduce
         (fn [out k]
           (let [x (load-fn (assoc k :use-cache true))]
             (cond-> out x (assoc k x))))
         {}
         (transit/read (transit/reader (io/input-stream ".portal/cache/boot.json") :json))))
       (.toString out))}
    (or (cache-get request) {:status 404})))

(defmethod route [:post "/cache"] [request]
  (fs/mkdir ".portal/cache")
  (let [path (->cache-key request)]
    (fs/spit path (slurp (:body request)))
    {:status 204}))

(defmethod route [:options "/load"] [_] enable-cors)
(defmethod route [:post "/load"] [request]
  (if-let [body (load-fn (assoc (body request)
                                :use-cache (= :boot (-> request :session :options :mode))))]
    {:status 200
     :headers
     {"content-type" "application/transit+json"}
     :body (let [out (ByteArrayOutputStream. 1024)]
             (transit/write
              (transit/writer out :json {:transform transit/write-meta})
              body)
             (.toString out))}
    {:status 400}))

(defmethod route [:options "/submit"] [_] enable-cors)
(defmethod route [:post "/submit"] [request]
  (rt/update-value (body request))
  {:status  204})

(defmethod route [:get "/"] [request]
  (if-let [session (:session request)]
    (send-resource "text/html" (index/html (:options session)))
    (let [session-id (UUID/randomUUID)]
      (swap! rt/sessions assoc session-id {})
      {:status 307 :headers {"Location" (str "?" session-id)}})))

(defn handler [request]
  (-> (with-session request)
      (route)
      (update :headers assoc "Access-Control-Allow-Origin" "*")))