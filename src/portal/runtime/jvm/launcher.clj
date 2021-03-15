(ns portal.runtime.jvm.launcher
  (:require [cheshire.core :as json]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [org.httpkit.server :as http]
            [portal.runtime :as rt]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server])
  (:import [java.util UUID]
           [java.io File FilenameFilter]))

(defn- random-uuid [] (UUID/randomUUID))

(defn- get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"
    "/mnt/c/Program Files (x86)/Google/Chrome/Application"]
   (s/split (System/getenv "PATH")
            (re-pattern (or (System/getProperty "path.separator") ":")))))

(defn- find-bin [files]
  (some
   identity
   (for [file files path (get-paths)]
     (let [f (io/file path file)]
       (when (and (.exists f) (.canExecute f))
         (.getAbsolutePath f))))))

(defn- get-chrome-bin []
  (find-bin ["chrome" "chrome.exe" "google-chrome-stable" "chromium" "Google Chrome"]))

(defonce ^:private server (atom nil))

(defn- get-app-id-profile-osx [app-name]
  (let [info (io/file (System/getProperty "user.home")
                      "Applications/Chrome Apps.localized/"
                      (str app-name ".app")
                      "Contents/Info.plist")]
    (when (.exists info)
      [(second (re-find #"com\.google\.Chrome\.app\.([^<]+)" (slurp info)))
       nil])))

(defn- get-app-id-from-pref-file [^File pref-file app-name]
  (when (.exists pref-file)
    (some
     (fn [[id extension]]
       (let [name (get-in extension ["manifest" "name"] "")]
         (when (= app-name name) id)))
     (get-in
      (json/parse-stream (io/reader pref-file))
      ["extensions" "settings"]))))

(defn- get-app-id-profile-linux [app-name]
  (let [chrome-config-dir (io/file
                           (System/getProperty "user.home")
                           ".config/google-chrome")
        pref-dirs (.listFiles
                   chrome-config-dir
                   (reify FilenameFilter
                     (accept [_ dir name]
                       (or (= "Default" name)
                           (some? (re-matches #"Profile\s\d+" name))))))
        pref-files (map
                    #(io/file % "Preferences")
                    pref-dirs)]
    (->> pref-files
         (some (fn [^File pref-file]
                 (when-let [app-id (get-app-id-from-pref-file pref-file app-name)]
                   (let [profile-name (->> pref-file
                                           .getPath
                                           (re-find #"\/([^/]+)/Preferences")
                                           second)]
                     [app-id profile-name])))))))

(defn- get-app-id-profile
  "Returns [app-id profile] tuple if portal is installed as `app-name` under any of the browser profiles"
  [app-name]
  (or (get-app-id-profile-osx app-name) (get-app-id-profile-linux app-name)))

(def pwa
  {:name "portal"
   :host "https://djblue.github.io/portal/"})

(defn- chrome-flags [url]
  (if-let [[app-id profile] (get-app-id-profile (:name pwa))]
    (->> [(str "--app-id=" app-id)
          (when profile
            (str "--profile-directory=" profile))
          (str "--app-launch-url-for-shortcuts-menu-item=" (:host pwa) "?" url)]
         (filter some?))
    ["--incognito"
     "--disable-features=TranslateUI"
     "--no-first-run"
     (str "--app=" url)]))

(defn start [options]
  (when (nil? @server)
    (let [{:portal.launcher/keys [port host] :or {port 0 host "localhost"}} options
          http-server (http/run-server #'server/handler
                                       {:port port
                                        :max-ws (* 1024 1024 1024)
                                        :legacy-return-value? false})]
      (reset!
       server
       {:http-server http-server
        :port (http/server-port http-server)
        :host host}))))

(defn- chrome [bin flags]
  (let [{:keys [exit err out]} (apply sh bin flags)]
    (when-not (zero? exit)
      (binding [*out* *err*]
        (println "Unable to open chrome:")
        (prn (into [bin] flags))
        (println err out)))))

(defn open
  ([options]
   (open nil options))
  ([portal options]
   (swap! rt/state merge options)
   (let [session-id (or (:session-id portal) (random-uuid))
         {:keys [host port]} (or @server (start nil))
         url (str "http://" host ":" port "?" session-id)]
     (when-not (c/open? session-id)
       (if-let [bin (get-chrome-bin)]
         (let [flags (chrome-flags url)]
           (future (chrome bin flags)))
         (try (browse-url url)
              (catch Exception _e
                (println "Goto" url "to view portal ui.")))))
     (c/make-atom session-id))))

(defn clear []
  (doseq [session-id (keys @c/sessions)]
    (c/request session-id {:op :portal.rpc/clear})))

(defn close []
  (doseq [session-id (keys @c/sessions)]
    (c/request session-id {:op :portal.rpc/close}))
  (some-> server deref :http-server http/server-stop!)
  (reset! server nil))
