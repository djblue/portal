(ns portal.extensions.vs-code
  (:require ["vscode" :as vscode]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.api :as p]
            [portal.async :as a]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.node.server :as server]))

(defn reload
  []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./vs-code")))

(defonce ^:private context (atom nil))

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
                               "0.19.0"])
                             (.-One vscode/ViewColumn)
                             #js {:enableScripts           true
                                  :retainContextWhenHidden true})
        ^js web-view        (.-webview panel)]
    (set! (.-iconPath panel)
          (.file vscode/Uri (.asAbsolutePath ^js @context "icon.png")))
    (set! (.-html web-view)
          (index/html :code-url   (str "http://" host ":" port "/main.js?" session-id)
                      :host       (str host ":" port)
                      :session-id (str session-id)))
    (.onDidReceiveMessage
     web-view
     (fn handle-message [^js message]
       (when-let [^js event (and (string? message) (js/JSON.parse message))]
         (case (.-type event)
           "close"     (.dispose panel)
           "set-title" (set! (.-title panel) (.-title event))
           "set-theme" :unsupported)))
     js/undefined
     (.-subscriptions ^js @context))))

(defmethod browser/-open :vs-code [args] (-open args))

(defn- get-workspace-folder []
  (-> vscode/workspace
      .-workspaceFolders
      (aget 0)
      (.. -uri -path)))

(defn- get-commands []
  {:extension.portalOpen
   (fn []
     (p/open {:launcher :vs-code}))
   :extension.portalOpenDev
   (fn []
     (let [path (fs/exists (str (get-workspace-folder) "/target/resources/portal/main.js"))]
       (p/open
        {:mode         :dev
         :window-title "vs-code-dev"
         :resource     {"main.js" path}
         :launcher     :vs-code})))})

(defn- get-body [^js req]
  (js/Promise.
   (fn [resolve reject]
     (let [body (atom "")]
       (.on req "data" #(swap! body str %))
       (.on req "end"  #(resolve @body))
       (.on req "error" reject)))))

(defmethod server/handler "/open" [req res]
  (a/let [body (get-body req)]
    (-open (edn/read-string body))
    (.end res)))

(defn- set-status [workspace]
  (when (fs/exists (str workspace "/target/resources/portal/main.js"))
    (.executeCommand vscode/commands "setContext" "portal:is-dev" true)))

(defn activate
  [^js ctx]
  (reset! context ctx)
  (a/let [workspace (get-workspace-folder)
          folder    (str workspace "/.portal")
          info      (p/start {})]
    (set-status workspace)
    (fs/mkdir folder)
    (fs/spit (str folder "/vs-code.edn")
             (pr-str (select-keys info [:host :port]))))
  (add-tap #'p/submit)
  (doseq [[command f] (get-commands)]
    (.push (.-subscriptions ctx)
           (.registerCommand vscode/commands (name command) f))))

(defn deactivate
  []
  (remove-tap #'p/submit))

(def exports #js {:activate activate
                  :deactivate deactivate})

(comment
  (p/open)
  (require '[examples.data])
  (tap> examples.data/data)
  (tap> 123)
  (p/close))

