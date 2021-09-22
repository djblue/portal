(ns portal.extensions.vs-code
  (:require ["vscode" :as vscode]
            [clojure.string :as str]
            [portal.api :as p]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]))

(defn reload
  []
  (.log js/console "Reloading...")
  (js-delete js/require.cache (js/require.resolve "./vs-code")))

(defmethod browser/-open :vs-code [{:keys [portal options server]}]
  (let [{:keys [host port]} server
        session-id          (:session-id portal)
        panel               (.createWebviewPanel
                             vscode/window
                             "portal"
                             (str/join
                              " - "
                              ["portal"
                               (get options :window-title "vs-code")
                               "0.15.1"])
                             (.-One vscode/ViewColumn)
                             #js {:enableScripts true})]
    (set! (.. panel -webview -html)
          (index/html :code-url   (str "http://" host ":" port "/main.js?" session-id)
                      :host       (str host ":" port)
                      :session-id (str session-id)))))

(defn- get-commands [context]
  (keep
   identity
   [(.registerCommand
     vscode/commands
     "extension.portalOpen"
     (fn []
       (p/open {:launcher :vs-code})))
    (when-let [path (fs/exists (.asAbsolutePath context "target/resources/portal/main.js"))]
      (.registerCommand
       vscode/commands
       "extension.portalOpenDev"
       (fn []
         (p/open
          {:mode         :dev
           :window-title "vs-code-dev"
           :resource     {"main.js" path}
           :launcher     :vs-code}))))]))

(defn activate
  [context]
  (add-tap #'p/submit)
  (doseq [command (get-commands context)]
    (.push (.-subscriptions context) command)))

(defn deactivate [])

(def exports #js {:activate activate
                  :deactivate deactivate})

(comment
  (p/open)
  (require '[examples.data])
  (tap> examples.data/data)
  (tap> 123)
  (p/close))

