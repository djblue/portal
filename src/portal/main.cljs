(ns portal.main
  (:require ["child_process" :as cp]
            ["fs" :as fs]
            ["http" :as http]
            ["path" :as path]
            [clojure.string :as s]
            [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.client :as c]
            [portal.runtime.transit :as t]))

(defn- get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"]
   (s/split (.-PATH js/process.env) #":")))

(defn- find-bin [files]
  (some
   identity
   (for [path (get-paths) file files]
     (let [f (path/join path file)]
       (when-not (try (fs/accessSync f fs/constants.X_OK)
                      (catch js/Error _e true))
         f)))))

(defn- get-chrome-bin []
  (find-bin #{"chrome" "google-chrome-stable" "chromium" "Google Chrome"}))

;; server.cljs


(defn- sh [bin & args]
  (js/Promise.
   (fn [resolve reject]
     (let [ps (cp/spawn bin (clj->js args))]
       (.on ps "error" reject)
       (.on ps "close" resolve)))))

(defonce server (atom nil))

(defn- buffer-body [request]
  (js/Promise.
   (fn [resolve reject]
     (let [body (atom "")]
       (.on request "data" #(swap! body str %))
       (.on request "end"  #(resolve @body))
       (.on request "error" reject)))))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- send-rpc [response value]
  (-> response
      (.writeHead 200 #js {"Content-Type"
                           "application/transit+json; charset=utf-8"})
      (.end (try
              (t/edn->json
               (assoc value :portal.rpc/exception nil))
              (catch js/Error e
                (t/edn->json
                 {:portal/state-id (:portal/state-id value)
                  :portal.rpc/exception e}))))))

(def ops (merge c/ops rt/ops))

(defn- rpc-handler [request response]
  (a/let [body    (buffer-body request)
          req     (t/json->edn body)
          op      (get ops (get req :op) not-found)
          done    #(send-rpc response %)
          cleanup (op req done)]
    (when (fn? cleanup)
      (.on request "close" cleanup))))

(defn- send-resource [response content-type body]
  (-> response
      (.writeHead 200 #js {"Content-Type" content-type})
      (.end body)))

(defn- handler [request response]
  (let [paths
        {"/"        #(send-resource response "text/html"       (io/resource "index.html"))
         "/main.js" #(send-resource response "text/javascript" (io/resource "main.js"))
         "/rpc"     #(rpc-handler request response)}

        f (get paths (.-url request) #(-> response (.writeHead 404) .end))]
    (when (fn? f) (f))))

(defn- start [handler]
  (js/Promise.
   (fn [resolve _reject]
     (let [server (http/createServer #(handler %1 %2))]
       (.listen server 0
                #(let [port (.-port (.address server))
                       result (with-meta {:server server} {:local-port port})]
                   (resolve result)))))))

(defn- stop [handle]
  (.close (:server handle)))

(defn open-inspector [options]
  (swap! rt/state merge {:portal/open? true} options)
  (a/let [chrome-bin (get-chrome-bin)
          instance   (or @server (start #'handler))
          url        (str "http://localhost:" (-> instance meta :local-port))]
    (reset! server instance)
    (if-not (some? chrome-bin)
      (println "Goto" url "to view portal ui.")
      (sh chrome-bin
          "--incognito"
          "--disable-features=TranslateUI"
          "--no-first-run"
          (str "--app=http://localhost:" (-> instance meta :local-port)))))
  true)

(defn close-inspector []
  (swap! rt/state assoc :portal/open? false)
  (stop @server)
  (reset! server nil)
  true)

(defn -main [])

