(ns sedit.main
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [cognitect.transit :as transit]
            [org.httpkit.server :as server]
            [org.httpkit.client :as client])
  (:import [java.io ByteArrayOutputStream PushbackReader]
           [java.util UUID]))

(defn get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"]
   (s/split (System/getenv "PATH") #":")))

(defn find-bin [files]
  (some
   identity
   (for [path (get-paths) file files]
     (let [f (io/file path file)]
       (when (and (.exists f) (.canExecute f))
         (.getAbsolutePath f))))))

(defn get-chrome-bin []
  (find-bin #{"chrome" "google-chrome-stable" "chromium" "Google Chrome"}))

(defn var->symbol [v]
  (let [m (meta v)]
    (symbol (str (:ns m)) (str (:name m)))))

(defn value->transit-stream [value out]
  (let [writer (transit/writer
                out
                :json
                {:handlers
                 {clojure.lang.Var
                  (transit/write-handler
                   "sedit.transit/var"
                   var->symbol)}
                 :default-handler
                 (transit/write-handler
                  "sedit.transit/unknown"
                  pr-str)})]
    (transit/write writer value)
    (.toString out)))

(defn transit-stream->value [in]
  (let [reader (transit/reader in :json)]
    (transit/read reader)))

(defn value->transit [value]
  (let [out (ByteArrayOutputStream. (* 10 1024 1024))]
    (value->transit-stream value out)
    (.toString out)))

(defn read-edn [reader]
  (with-open [in (java.io.PushbackReader. reader)] (edn/read in)))

(defonce state (atom nil))

(defn update-setting [k v] (swap! state assoc k v) true)

(defn update-value [new-value]
  (swap!
   state
   (fn [state]
     (assoc
      state
      :sedit/state-id (UUID/randomUUID)
      :sedit/value
      (let [value (:sedit/value state)]
        (if-not (coll? value)
          (list new-value)
          (conj value new-value)))))))

(defn clear-values []
  (swap! state assoc
         :sedit/state-id (UUID/randomUUID)
         :sedit/value (list)))

(defn send-rpc [channel value]
  (server/send!
   channel
   {:status 200
    :headers {"Content-Type"
              "application/transit+json; charset=utf-8"}
    :body (try
            (value->transit value)
            (catch Exception e
              (value->transit {:status :error})))}))

(def ops
  {:sedit.rpc/clear-values
   (fn [request channel]
     (send-rpc channel (clear-values)))
   :sedit.rpc/load-state
   (fn [request channel]
     (let [state-value @state
           id (get-in request [:body :sedit/state-id])]
       (if-not (= id (:sedit/state-id state-value))
         (send-rpc channel state-value)
         (let [watch-key (keyword (gensym))]
           (add-watch
            state
            watch-key
            (fn [_ _ old new]
              (send-rpc channel @state)))
           (server/on-close
            channel
            (fn [status]
              (remove-watch state watch-key)))))))
   :sedit.rpc/http-request
   (fn [request channel]
     (send-rpc
      channel
      @(client/request (get-in request [:body :request]))))})

(defn not-found [request channel]
  (send-rpc channel {:status :not-found}))

(defn rpc-handler [request]
  (server/with-channel request channel
    (let [request (update request :body transit-stream->value)
          op (get ops (get-in request [:body :op]) not-found)]
      (op request channel))))

(defn send-resource [content-type resource-name]
  {:status  200
   :headers {"Content-Type" content-type}
   :body    (-> resource-name io/resource io/file)})

(defn handler [request]
  (let [paths
        {"/"        #(send-resource "text/html"       "index.html")
         "/main.js" #(send-resource "text/javascript" "main.js")
         "/rpc"     #(rpc-handler request)}
        f (get paths (:uri request))]
    (when (fn? f) (f))))

(defonce server (atom nil))

(defn open-inspector [value]
  (swap! state assoc :sedit/open? true)
  (update-value value)
  (when (nil? @server)
    (reset!
     server
     (server/run-server #'handler {:port 0 :join? false})))
  (sh (get-chrome-bin)
      "--incognito"
      "--disable-features=TranslateUI"
      "--no-first-run"
      (str "--app=http://localhost:" (-> @server meta :local-port))))

(defn close-inspector []
  (swap! state assoc :sedit/open? false)
  (@server :timeout 1000)
  (reset! server nil))

(defn inspect [v]
  (.start (Thread. #(open-inspector v)))
  v)

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"  (-> *in* io/reader (json/read :key-fn keyword))
             "edn"   (-> *in* io/reader read-edn))]
    (open-inspector in)
    (shutdown-agents)))
