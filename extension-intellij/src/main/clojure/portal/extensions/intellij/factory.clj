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

; instances are indexed per project, each instance will contain the keys
; :browser and :server
(defonce ^:private instances (atom {}))

(defn- format-url [{:keys [portal server]}]
  (str "http://" (:host server) ":" (:port server) "?" (:session-id portal)))

(defn- run-js [^JBCefBrowser browser ^String js]
  (when browser
    (.executeJavaScript (.getCefBrowser browser) js "" 0)))

(defn- get-options []
  (pr-str
   (pr-str
    {:portal.colors/theme ::theme
     :themes
     {::theme (theme/get-theme)}})))

(defn init-options [browser]
  (run-js browser
   (str
    "sessionStorage.setItem(\"PORTAL_EXTENSION_OPTIONS\", " (get-options) ")")))

(defn patch-options
  ([]
   (doseq [browser (into [] (map :browser) (vals @instances))]
     (patch-options browser)))
  ([browser]
   (init-options browser)
   (run-js browser
     (str "portal.ui.options.patch(" (get-options) ")"))))

(defn -uiSettingsChanged  [_this _] (patch-options))
(defn -globalSchemeChange [_this _] (patch-options))

(defn handler [request project]
  (let [body (edn/read (PushbackReader. (io/reader (:body request))))
        browser (get-in @instances [project :browser])]
    (case (:uri request)
      "/open"
      (do (init-options browser)
          (.loadURL ^JBCefBrowser browser (format-url body))
          {:status 200})
      "/open-file" (do (file/open project body) {:status 200})
      {:status 404})))

(defn- write-config [^Project project config]
  (let [dir  (.getCanonicalPath (.getBaseDir project))
        file (io/file dir ".portal" "intellij.edn")]
    (.mkdirs (.getParentFile file))
    (spit file (pr-str config))))

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

(defn- get-window ^JComponent [^Project project]
  (start project)
  (let [b (JBCefBrowser.)]
    (swap! instances assoc-in [project :browser] b)
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
