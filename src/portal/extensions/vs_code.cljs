(ns portal.extensions.vs-code
  (:require ["vscode" :as vscode :refer [notebooks]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.api :as p]
            [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.node.server :as server]))

(defonce ^:private !app-db (atom {:context nil
                                  :disposables []}))

(defn- register-disposable! [^js disposable]
  (let [context (:context @!app-db)]
    (swap! !app-db update :disposables conj disposable)
    (.push (.-subscriptions context) disposable)))

(defn- clear-disposables! []
  (doseq [disposable (:disposables @!app-db)]
    (.dispose disposable))
  (swap! !app-db assoc :disposables []))

(def ^:private view-column-key "portal:viewColumn")

(defn- view-column []
  (let [^js context (:context @!app-db)
        column (-> context .-workspaceState (.get view-column-key))]
    (if-not (= js/undefined column)
      column
      (.-Beside vscode/ViewColumn))))

(defn- save-view-column [column]
  (let [^js context (:context @!app-db)]
    (-> context .-workspaceState (.update view-column-key column))))

(defn- -open [{:keys [portal options server]}]
  (let [{:keys [host port]} server
        session-id          (:session-id portal)
        ^js panel           (.createWebviewPanel
                             vscode/window
                             "portal"
                             (str/join
                              " - "
                              ["portal"
                               (get options :window-title "vs-code")
                               "0.59.2"])
                             (view-column)
                             (clj->js
                              {:enableScripts           true
                               :retainContextWhenHidden true
                               :portMapping
                               [{:webviewPort port
                                 :extensionHostPort port}]}))
        ^js web-view        (.-webview panel)]
    (set! (.-iconPath panel)
          (.file vscode/Uri (.asAbsolutePath ^js (:context @!app-db) "icon.png")))
    (set! (.-html web-view)
          (index/html {:code-url   (str "http://" host ":" port "/main.js?" session-id)
                       :host       host
                       :port       port
                       :session-id (str session-id)}))
    (.onDidReceiveMessage
     web-view
     (fn handle-message [^js message]
       (when-let [^js event (and (string? message) (js/JSON.parse message))]
         (case (.-type event)
           "close"     (.dispose panel)
           "set-title" (set! (.-title panel) (.-title event))
           "set-theme" :unsupported)))
     js/undefined
     (.-subscriptions ^js (:context @!app-db)))
    (.onDidChangeViewState
     panel
     (fn [_event]
       (save-view-column (.-viewColumn panel))))))

(defmethod browser/-open :vs-code [args] (-open args))

(defn- get-workspace-folder []
  (let [^js uri (-> vscode/workspace .-workspaceFolders (aget 0) .-uri)
        fs-path (.-fsPath uri)]
    (if-not (undefined? fs-path) fs-path (.-path uri))))

(defn- get-commands []
  {:extension.portalOpen
   (fn []
     (p/open {:launcher :vs-code}))
   :extension.portalOpenDev
   (fn []
     (let [path (fs/exists (fs/join (get-workspace-folder) "resources/portal-dev/main.js"))]
       (p/open
        {:mode         :dev
         :window-title "vs-code-dev"
         :resource     {"main.js" path}
         :launcher     :vs-code})))})

(defn- setup-notebook-handler []
  (let [message-channel (.createRendererMessaging notebooks "portal-edn-renderer")]
    (.onDidReceiveMessage
     message-channel
     (fn handle-message [^js event]
       (let [message (.-message event)]
         (case (.-type message)
           "open-editor"
           (p/open
            {:launcher     :vs-code
             :window-title "notebook"
             :value        (edn/read-string
                            {:default tagged-literal}
                            (.-data message))})))))))

(defmethod server/route [:post "/open"] [req ^js res]
  (a/let [body (server/get-body req)]
    (-open (edn/read-string body))
    (.end res)))

(defn- open-file* [{:keys [file line column]}]
  (a/let [document     (.openTextDocument
                        vscode/workspace
                        (.file vscode/Uri file))
          ^js editor   (.showTextDocument vscode/window document 1 false)
          ^js position (vscode/Position. (dec line) (dec column))
          ^js range    (vscode/Range. position position)]
    (set! (.-selection editor) (vscode/Selection. (.-start range) (.-end range)))
    (.revealRange editor range)))

(defn- open-file [{:keys [file] :as opts}]
  (a/try
    (open-file* opts)
    (catch :default _
      (open-file* (assoc opts :file (fs/join (get-workspace-folder) file))))))

(defmethod server/route [:post "/open-file"] [req ^js res]
  (a/try
    (a/let [body (server/get-body req)]
      (open-file (edn/read-string body))
      (.end res))
    (catch :default _e
      (doto res
        (.writeHead 400 #js {})
        (.end)))))

(defn- set-status [workspace]
  (when (fs/exists (fs/join workspace "resources/portal-dev/main.js"))
    (.executeCommand vscode/commands "setContext" "portal:is-dev" true)))

(def ^:private portal-source
  [(io/inline "portal/async.cljc")
   (io/inline "portal/runtime/datafy.cljc")
   (io/inline "portal/runtime/json_buffer.cljc")
   (io/inline "portal/runtime/macros.cljc")
   (io/inline "portal/runtime/cson.cljc")
   (io/inline "portal/viewer.cljc")
   (io/inline "portal/runtime.cljc")
   (io/inline "portal/runtime/json.cljc")
   (io/inline "portal/runtime/transit.cljc")
   (io/inline "portal/client/common.cljs")
   (io/inline "portal/client/node.cljs")
   (io/inline "portal/runtime/fs.cljc")
   (io/inline "portal/runtime/node/client.cljs")
   (io/inline "portal/runtime/rpc.cljc")
   (io/inline "portal/runtime/shell.cljc")
   (io/inline "portal/runtime/browser.cljc")
   (io/inline "portal/resources.cljc")
   (io/inline "portal/runtime/index.cljc")
   (io/inline "portal/runtime/node/server.cljs")
   (io/inline "portal/runtime/node/launcher.cljs")
   (io/inline "portal/api.cljc")
   (io/inline "portal/console.cljc")])

(defn- get-extension ^js/Promise [extension-name]
  (js/Promise.
   (fn [resolve reject]
     (let [n (atom 16) delay 250]
       (js/setTimeout
        (fn work []
          (try
            (resolve (.-exports (.getExtension vscode/extensions extension-name)))
            (catch :default e
              (if (zero? (swap! n dec))
                (reject (ex-info "Max attempts reached" {} e))
                (js/setTimeout work delay)))))
        delay)))))

(defn- setup-joyride! []
  (a/try
    (a/let [^js joyride (get-extension "betterthantomorrow.joyride")]
      (reduce
       (fn [^js/Promise chain source]
         (-> chain
             (.then
              (fn [_]
                (.runCode joyride source)))))
       (.resolve js/Promise 0)
       portal-source))
    (catch :default e
      (.error js/console e)))
  (clj->js {:resources {:inline io/inline}}))

(defn activate
  [^js ctx]
  (when ctx
    (swap! !app-db assoc :context ctx)
    (a/let [workspace (get-workspace-folder)
            folder    (fs/join workspace ".portal")
            info      (p/start {})
            config    (fs/join folder "vs-code.edn")]
      (set-status workspace)
      (fs/mkdir folder)
      (fs/spit config (pr-str (select-keys info [:host :port]))))
    (setup-notebook-handler)
    (add-tap #'p/submit))
  (doseq [[command f] (get-commands)]
    (register-disposable! (vscode/commands.registerCommand (name command) f)))
  (setup-joyride!))

(defn deactivate
  []
  (remove-tap #'p/submit))

(def exports #js {:activate activate
                  :deactivate deactivate})

(defn before-load
  [done-fn]
  (.log js/console "Reloading...")
  (clear-disposables!)
  (js-delete js/require.cache (js/require.resolve "./vs-code"))
  (when done-fn
    (done-fn)))

(defn after-load
  []
  (.log js/console "Reloaded, reactivating...")
  (activate nil))

(comment
  (p/open)
  (require '[examples.data])
  (tap> examples.data/data)
  (tap> 123)
  (p/close))
