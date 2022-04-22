(ns portal.extensions.electron
  (:require ["electron" :refer [BrowserWindow app]]
            [clojure.edn :as edn]
            [portal.api :as p]
            [portal.async :as a]
            [portal.runtime.fs :as fs]
            [portal.runtime.node.server :as server]))

(defonce ^:private window (atom nil))

(defn- create-window []
  (BrowserWindow.
   #js {:title          "portal"
        :width          1200
        :height         600
        ;; :titleBarStyle "hiddenInset"
        :transparent    true
        :alwaysOnTop    true
        :frame          false
        :opacity        0.75
        :webPreferences #js {:zoomFactor 1.5}
        :show           false}))

(def ^:private close-timeout (* 60 60 1000))

(defn- lazy-close [^js window]
  (let [force? (atom false)]
    (.once app "before-quit"
           (fn []
             (js/console.log "app quit")
             (reset! force? true)
             (when-not (.isDestroyed window)
               (.close window))))
    (.on window "close"
         (fn [e]
           (when-not @force?
             (.preventDefault e)
             (.hide window)
             (let [timeout (js/setTimeout
                            (fn []
                              (js/console.log "destroying")
                              (.destroy window))
                            close-timeout)]
               (.once window "show"
                      (fn []
                        (js/console.log "prevent destroy")
                        (js/clearTimeout timeout)))))))
    window))

(defn- ->url [{:keys [portal server]}]
  (let [{:keys [host port]} server
        session-id          (:session-id portal)]
    (str "http://" host ":" port "/?" session-id)))

(defn- get-url [^js window]
  (some-> window .-webContents .getURL))

(defn- open [options]
  (when (or (not @window) (.isDestroyed @window))
    (reset! window (lazy-close (create-window))))
  (let [url (->url options)]
    (when-not (= (get-url @window) url)
      (prn [(get-url @window) url])
      (.loadURL @window url))
    (.setVisibleOnAllWorkspaces @window true)
    (.showInactive @window)
    (.setVisibleOnAllWorkspaces @window false)))

(defmethod server/route [:post "/open"] [req res]
  (a/let [body (server/get-body req)]
    (open (edn/read-string body))
    (.end res)))

(.on app "browser-window-focus"
     (fn [e]
       (.setOpacity (.-sender e) 1.0)))
(.on app "browser-window-blur"
     (fn [e]
       (.setOpacity (.-sender e) 0.85)))

(.on app "window-all-closed"
     #(when-not (= js/process.platform "darwin")
        (.quit app)))

(defonce ^:private init? (atom false))

(defn -main []
  (when-not @init?
    (a/let [_      (.whenReady app)
            info   (p/start {})
            folder (fs/join (fs/home) ".portal")
            config (fs/join folder "electron.edn")]
      (fs/mkdir folder)
      (fs/spit config (pr-str (select-keys info [:host :port])))
      (fs/rm-exit config))))

(-main)

(defn reload [])
