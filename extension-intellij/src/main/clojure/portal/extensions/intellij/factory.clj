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
   (org.cef.browser CefBrowser)
   (org.cef.handler CefLoadHandler)
   (portal.extensions.intellij WithLoader))
  (:gen-class
   :main false
   :extends portal.extensions.intellij.WithLoader
   :implements [com.intellij.ide.ui.UISettingsListener
                com.intellij.openapi.editor.colors.EditorColorsListener
                com.intellij.openapi.wm.ToolWindowFactory
                com.intellij.openapi.project.DumbAware]
   :name portal.extensions.intellij.Factory))

; instances are indexed per project, each instance will contain the keys
; :browser and :server
(defonce ^:private instances (atom {}))

(defn- format-url [{:keys [portal server]}]
  (str "http://" (:host server) ":" (:port server) "?" (:session-id portal)))

(defn- get-options []
  (pr-str
   (pr-str
    {:theme ::theme
     :themes
     {::theme (theme/get-theme)}})))

(defn- patch-options
  ([]
   (doseq [instance (vals @instances)]
     (patch-options (.getCefBrowser ^JBCefBrowser (:browser instance)))))
  ([^CefBrowser browser]
   (.executeJavaScript browser (str "portal.ui.options.patch(" (get-options) ")") "" 0)))

(defn -uiSettingsChanged  [_this _] (patch-options))
(defn -globalSchemeChange [_this _] (patch-options))

(defn handler [request project]
  (let [body (edn/read (PushbackReader. (io/reader (:body request))))
        browser (get-in @instances [project :browser])]
    (case (:uri request)
      "/open"
      (do (.loadURL ^JBCefBrowser browser (format-url body))
          {:status 200})
      "/open-file" (do (file/open project body) {:status 200})
      {:status 404})))

(defn- write-config [^Project project config]
  (let [dir  (.getCanonicalPath (.getBaseDir project))
        file (io/file dir ".portal" "intellij.edn")]
    (.mkdirs (.getParentFile file))
    (spit file (pr-str config))
    (.deleteOnExit file)))

(defn start [^Project project]
  (swap! instances update project
         (fn [m]
           (cond-> m
             (not (:server m))
             (assoc :server (http/run-server #(handler % project) {:port 0 :legacy-return-value? false})))))
  (write-config
   project
   {:host "localhost"
    :port (http/server-port (get-in @instances [project :server]))}))

(defn- init-browser [^JBCefBrowser browser]
  (.addLoadHandler
   (.getJBCefClient browser)
   (reify CefLoadHandler
     (onLoadingStateChange [_this _browser _isLoading _canGoBack _canGoForward])
     (onLoadStart [_this _browser _frame _transitionType])
     (onLoadEnd [_this browser _frame _httpStatusCode]
       (patch-options browser))
     (onLoadError [_this _browser _frame _errorCode _errorText _failedUrl]))
   (.getCefBrowser browser)))

(defn- get-window ^JComponent [^Project project]
  (start project)
  (let [browser (JBCefBrowser.)]
    (init-browser browser)
    (swap! instances assoc-in [project :browser] browser)
    (Disposer/register project browser)
    (.getComponent browser)))

(defn get-nrepl []
  (try
    (requiring-resolve 'nrepl.server/start-server)
    (catch Exception _e)))

;; Methods for: com.intellij.openapi.wm.ToolWindowFactory
;; - https://github.com/JetBrains/intellij-community/blob/master/platform/platform-api/src/com/intellij/openapi/wm/ToolWindowFactory.kt

(defn -init [_this ^ToolWindow _window]
  (WithLoader/bind)
  (when-let [start-server (get-nrepl)] (start-server :port 7888)))

(defn -isApplicable [_this ^Project _project] true)
(defn -isApplicableAsync [_this ^Project _project _] true)
(defn -shouldBeAvailable [_this ^Project _project] true)
(defn -manage [_this ^ToolWindow _window _ _])

(defn -getIcon [_this])
(defn -getAnchor [_this])

(defn -createToolWindowContent [_this ^Project project ^ToolWindow window]
  (let [component (.getComponent window)]
    (.add component (get-window project))))
