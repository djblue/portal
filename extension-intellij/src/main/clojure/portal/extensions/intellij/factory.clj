(ns portal.extensions.intellij.factory
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.httpkit.server :as http]
            [portal.extensions.intellij.file :as file]
            [portal.extensions.intellij.theme :as theme])
  (:import
   (com.intellij.openapi.project Project)
   (com.intellij.openapi.util Disposer)
   (com.intellij.openapi.wm ToolWindow)
   (com.intellij.ui.jcef JBCefBrowser)
   (java.io PushbackReader)
   (javax.swing JComponent)
   (portal.extensions.intellij WithLoader))
  (:gen-class
   :main false
   :extends portal.extensions.intellij.WithLoader
   :implements [com.intellij.ide.ui.UISettingsListener
                com.intellij.openapi.editor.colors.EditorColorsListener
                com.intellij.openapi.wm.ToolWindowFactory]
   :name portal.extensions.intellij.Factory))

(defonce ^:private browser (atom nil))
(defonce ^:private proj    (atom nil))
(defonce ^:private server  (atom nil))

(defn- format-url [{:keys [portal server]}]
  (str "http://" (:host server) ":" (:port server) "?" (:session-id portal)))

(defn- run-js [^String js]
  (when-let [^JBCefBrowser browser @browser]
    (.executeJavaScript (.getCefBrowser browser) js "" 0)))

(defn- get-options []
  (pr-str
   (pr-str
    {:portal.colors/theme ::theme
     :themes
     {::theme (theme/get-theme)}})))

(defn patch-options []
  (run-js
   (str "portal.ui.options.patch(" (get-options) ")")))

(defn init-options []
  (run-js
   (str "window.PORTAL_EXTENSION_OPTIONS = " (get-options))))

(defn -uiSettingsChanged  [_this _] (patch-options))
(defn -globalSchemeChange [_this _] (patch-options))

(defn handler [request]
  (let [body (edn/read (PushbackReader. (io/reader (:body request))))]
    (case (:uri request)
      "/open"
      (do (.loadURL ^JBCefBrowser @browser (format-url body))
          (init-options)
          {:status 200})
      "/open-file" (do (file/open @proj body) {:status 200})
      {:status 404})))

(defn- write-config [^Project project config]
  (let [dir  (.getCanonicalPath (.getBaseDir project))
        file (io/file dir ".portal" "intellij.edn")]
    (.mkdirs (.getParentFile file))
    (spit file (pr-str config))))

(defn start [^Project project]
  (when-not @server
    (reset! server (http/run-server #'handler {:port 0 :legacy-return-value? false})))
  (write-config
   project
   {:host "localhost"
    :port (http/server-port @server)}))

(defn- get-window ^JComponent [^Project project]
  (start project)
  (reset! proj project)
  (let [b (JBCefBrowser.)]
    (reset! browser b)
    (Disposer/register project b)
    (.getComponent b)))

(defn get-nrepl []
  (try
    (requiring-resolve 'nrepl.server/start-server)
    (catch Exception _e)))

(defn -init [_this ^ToolWindow _window]
  (WithLoader/bind)
  (when-let [start-server (get-nrepl)] (start-server :port 7888)))

(defn -isApplicable [_this ^Project _project] true)
(defn -shouldBeAvailable [_this ^Project _project] true)

(defn -createToolWindowContent [_this ^Project project ^ToolWindow window]
  (let [component (.getComponent window)]
    (.add (.getParent component) (get-window project))))
