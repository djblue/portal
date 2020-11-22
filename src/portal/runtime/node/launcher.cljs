(ns portal.runtime.node.launcher
  (:require ["child_process" :as cp]
            ["fs" :as fs]
            ["http" :as http]
            ["path" :as path]
            [clojure.string :as s]
            [portal.async :as a]
            [portal.runtime :as rt]
            [portal.runtime.node.server :as server]))

(defn- get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"
    "/mnt/c/Program Files (x86)/Google/Chrome/Application"]
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
  (find-bin #{"chrome" "chrome.exe" "google-chrome-stable" "chromium" "Google Chrome"}))

(defn- sh [bin & args]
  (js/Promise.
   (fn [resolve reject]
     (let [ps (cp/spawn bin (clj->js args))]
       (.on ps "error" reject)
       (.on ps "close" resolve)))))

(defonce ^:private server (atom nil))

(defn- create-server [handler port host]
  (js/Promise.
   (fn [resolve _reject]
     (let [server (http/createServer #(handler %1 %2))]
       (.listen server port
                #(let [port (.-port (.address server))
                       result {:http-server server
                               :port port
                               :host host}]
                   (resolve result)))))))

(defn start [options]
  (when (nil? @server)
    (a/let [{:portal.launcher/keys [port host] :or {port 0 host "localhost"}} options
            instance (create-server #'server/handler port host)]
      (reset! server instance))))

(defn- stop [handle]
  (some-> handle :http-server .close))

(defn open [options]
  (let [session-id (random-uuid)]
    (swap! rt/state merge {:portal/open? true} options)
    (a/let [chrome-bin (get-chrome-bin)
            {:keys [host port]} (or @server (start nil))
            url        (str "http://" host ":" port "?" session-id)]
      (if-not (some? chrome-bin)
        (println "Goto" url "to view portal ui.")
        (sh chrome-bin
            "--incognito"
            "--disable-features=TranslateUI"
            "--no-first-run"
            (str "--app=" url))))
    {:session-id session-id}))

(defn close []
  (swap! rt/state assoc :portal/open? false)
  (stop @server)
  (reset! server nil)
  true)

