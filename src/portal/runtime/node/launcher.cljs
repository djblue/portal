(ns portal.runtime.node.launcher
  (:require ["child_process" :as cp]
            ["fs" :as fs]
            ["http" :as http]
            ["path" :as path]
            [clojure.string :as s]
            [portal.async :as a]
            [portal.runtime :as rt]
            [portal.runtime.node.client :as c]
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
(defonce ^:private sockets (atom #{}))

(defn- create-server [handler port host]
  (js/Promise.
   (fn [resolve _reject]
     (let [server (http/createServer #(handler %1 %2))]
       (.on server
            "connection"
            (fn [socket]
              (swap! sockets conj socket)
              (.on socket
                   "close"
                   (fn [] (swap! sockets disj sockets)))))
       (.listen server port
                #(let [port (.-port (.address server))
                       result {:http-server server
                               :port port
                               :host host}]
                   (resolve result)))))))

(defn start [options]
  (or @server
      (a/let [{:portal.launcher/keys [port host]
               :or {port 0 host "localhost"}} options
              instance (create-server #'server/handler port host)]

        (reset! server instance))))

(defn- stop [handle]
  (some-> handle :http-server .close))

(defn browse-url [url]
  (case (.-platform js/process)
    ("android" "linux") (sh "xdg-open" url)
    "darwin"            (sh "open" url)
    "win32"             (sh "cmd" "/c" "start" url)
    (println "Goto" url "to view portal ui.")))

(defn open
  ([options]
   (open nil options))
  ([portal options]
   (let [session-id (or (:session-id portal) (random-uuid))]
     (swap! rt/state merge options)
     (a/let [{:keys [host port]} (start options)
             url        (str "http://" host ":" port "?" session-id)
             chrome-bin (get-chrome-bin)]
       (when-not (c/open? session-id)
         (if (and (some? chrome-bin)
                  (:portal.launcher/app options true))
           (sh chrome-bin
               "--incognito"
               "--disable-features=TranslateUI"
               "--no-first-run"
               (str "--app=" url))
           (browse-url url))))
     {:session-id session-id})))

(defn clear []
  (js/Promise.all
   (for [session-id (keys @c/sessions)]
     (c/request session-id {:op :portal.rpc/clear}))))

(defn close []
  (a/do
    (js/Promise.all
     (for [session-id (keys @c/sessions)]
       (c/request session-id {:op :portal.rpc/close})))
    (doseq [socket @sockets] (.destroy socket))
    (reset! sockets #{})
    (stop @server)
    (reset! server nil))
  true)

