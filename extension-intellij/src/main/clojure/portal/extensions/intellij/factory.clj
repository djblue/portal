(ns portal.extensions.intellij.factory
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [org.httpkit.server :as http]
            [portal.extensions.intellij.file :as file]
            [portal.extensions.intellij.theme :as theme])
  (:import
   (com.intellij.openapi.diagnostic Logger)
   (com.intellij.openapi.project Project)
   (com.intellij.openapi.util Disposer)
   (com.intellij.openapi.wm ToolWindow)
   (com.intellij.ui.jcef JBCefBrowser JBCefJSQuery)
   (java.io PushbackReader)
   (java.util.function Function)
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

(deftype PortalLogger [])

(def LOG (Logger/getInstance PortalLogger))

(defn info [message]
  (.info ^Logger LOG ^String message))

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
   (let [js (str "portal.ui.options.patch(" (get-options) ")")]
     (info (str "Running " js))
     (.executeJavaScript browser js "" 0))))

(defn -uiSettingsChanged  [_this _] (patch-options))
(defn -globalSchemeChange [_this _] (patch-options))

(defn handler [request project]
  (info (str "Request: " (:uri request)))
  (let [body (edn/read (PushbackReader. (io/reader (:body request))))
        browser (get-in @instances [project :browser])]
    (case (:uri request)
      "/open"
      (let [url (format-url body)]
        (info (str "Opening " url))
        (.loadURL ^JBCefBrowser browser url)
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
  (info "Starting Portal plugin")
  (swap! instances update project
         (fn [m]
           (cond-> m
             (not (:server m))
             (assoc :server (http/run-server #(handler % project) {:port 0 :legacy-return-value? false})))))
  (write-config
   project
   {:host "localhost"
    :port (http/server-port (get-in @instances [project :server]))}))

(defn as-function ^Function [f]
  (reify Function
    (apply [_this arg] (f arg))))

(defn- setup-java-error-handler [^JBCefBrowser browser]
  (doto (JBCefJSQuery/create browser)
    (.addHandler (as-function #(throw (ex-info (str "JavaScript error: " %)
                                               {}))))))

(defn- inject-js-error-handler [^JBCefBrowser browser ^JBCefJSQuery js-query]
  (let [js-fn (str "window.portal_reportError = function(error) { "
                   (.inject js-query "error")
                   "};"
                   "window.addEventListener('error', function(e) {"
                   "  window.portal_reportError(e.error.stack);"
                   "  return false;"
                   "}, true);"
                   "window.addEventListener('unhandledrejection', function(e) {"
                   "  window.portal_reportError(e.reason.stack);"
                   "  return false;"
                   "});")]
    (.executeJavaScript (.getCefBrowser browser) js-fn "" 0)))

(defn- setup-load-handler [^JBCefBrowser browser js-query]
  (.addLoadHandler
   (.getJBCefClient browser)
   (reify CefLoadHandler
     (onLoadingStateChange [_this _browser _isLoading _canGoBack _canGoForward])
     (onLoadStart [_this _browser _frame _transitionType]
       (info "Starting loading")
       (inject-js-error-handler browser js-query))
     (onLoadEnd [_this browser _frame _httpStatusCode]
       (info "Patching options")
       (patch-options browser))
     (onLoadError [_this _browser _frame errorCode errorText failedUrl]
       (throw (ex-info errorText {:errorCode errorCode
                                  :errorText errorText
                                  :failedUrl failedUrl}))))
   (.getCefBrowser browser)))

(defn- init-browser [^JBCefBrowser browser]
  (info "Initializing browser")
  (let [js-query (setup-java-error-handler browser)]
    (setup-load-handler browser js-query)))

(defn- get-window ^JComponent [^Project project]
  (info "Getting window")
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
  (when-let [start-server (get-nrepl)]
    (info "Starting REPL")
    (start-server :port 7888)))

(defn -isApplicable [_this ^Project _project] true)
(defn -isApplicableAsync [_this ^Project _project _] true)
(defn -shouldBeAvailable [_this ^Project _project] true)
(defn -manage [_this ^ToolWindow _window _ _])

(defn -getIcon [_this])
(defn -getAnchor [_this])

(defn -createToolWindowContent [_this ^Project project ^ToolWindow window]
  (let [component (.getComponent window)]
    (.add component (get-window project))))
