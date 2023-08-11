(ns ^:no-doc portal.runtime.browser
  #?(:clj  (:refer-clojure :exclude [random-uuid]))
  #?(:clj  (:require [clojure.java.browse :refer [browse-url]]
                     [portal.runtime :as rt]
                     [portal.runtime.fs :as fs]
                     [portal.runtime.json :as json]
                     [portal.runtime.jvm.client :as c]
                     [portal.runtime.shell :as shell])
     :cljs (:require [portal.runtime :as rt]
                     [portal.runtime.fs :as fs]
                     [portal.runtime.json :as json]
                     [portal.runtime.node.client :as c]
                     [portal.runtime.shell :as shell])
     :cljr (:require [portal.runtime :as rt]
                     [portal.runtime.clr.client :as c]
                     [portal.runtime.fs :as fs]
                     [portal.runtime.json :as json]
                     [portal.runtime.shell :as shell]))
  #?(:cljr (:import [System.Runtime.InteropServices RuntimeInformation OSPlatform])))

(defmulti -open (comp :launcher :options))

(defn- get-chrome-bin []
  (fs/find-bin
   (concat
    ["/Applications/Google Chrome.app/Contents/MacOS"
     "/Program Files/Google/Chrome/Application"
     "/Program Files (x86)/Google/Chrome/Application"
     "/mnt/c/Program Files/Google/Chrome/Application"
     "/mnt/c/Program Files (x86)/Google/Chrome/Application"]
    (fs/paths))
   ["chrome" "chrome.exe" "google-chrome-stable" "chromium" "Google Chrome"]))

(defn- get-app-id-profile-osx [app-name]
  (let [info (fs/join
              (fs/home)
              "Applications/Chrome Apps.localized/"
              (str app-name ".app")
              "Contents/Info.plist")]
    (when-let [app-id (some->> info
                               fs/exists
                               fs/slurp
                               (re-find #"com\.google\.Chrome\.app\.([^<]+)")
                               second)]
      {:app-id app-id})))

(defn- get-app-id-from-pref-file [path app-name]
  (when (fs/exists path)
    (some
     (fn [[id extension]]
       (let [name (get-in extension ["manifest" "name"] "")]
         (when (= app-name name) id)))
     (get-in
      (json/read (fs/slurp path))
      ["extensions" "settings"]))))

(defn- chrome-profile [path]
  (re-find #"Default|Profile\s\d+$" path))

(defn- get-app-id-profile-linux [app-name]
  (when-let [chrome-config-dir (-> (fs/home)
                                   (fs/join ".config" "google-chrome")
                                   fs/exists)]
    (first
     (for [file  (fs/list chrome-config-dir)
           :let  [profile     (chrome-profile file)
                  preferences (fs/join file "Preferences")
                  app-id      (get-app-id-from-pref-file preferences app-name)]
           :when (and profile app-id)]
       {:app-id app-id :profile profile}))))

(defn- get-app-id-profile
  "Returns app-id and profile if portal is installed as `app-name` under any of the browser profiles"
  [app-name]
  (or (get-app-id-profile-osx app-name) (get-app-id-profile-linux app-name)))

(def pwa
  {:name "portal"
   :host "https://djblue.github.io/portal/"})

(defn- flags [url]
  (if-let [{:keys [app-id profile]} (get-app-id-profile (:name pwa))]
    (->> [(str "--app-id=" app-id)
          (when profile
            (str "--profile-directory=" profile))
          (str "--app-launch-url-for-shortcuts-menu-item=" (:host pwa) "?" url)]
         (filter some?))
    [(str "--app=" url)]))

(defn- browse [url]
  #?(:clj
     (try (browse-url url)
          (catch Exception _e
            (println "Goto" url "to view portal ui.")))
     :cljs
     (case (.-platform js/process)
       ("android" "linux") (shell/sh "xdg-open" url)
       "darwin"            (shell/sh "open" url)
       "win32"             (shell/sh "cmd" "/c" "start" url)
       (println "Goto" url "to view portal ui."))
     :cljr
     (condp identical? (.Platform Environment/OSVersion)
       PlatformID/Win32NT      (shell/sh "cmd" "/c" "start" url)
       PlatformID/Win32Windows (shell/sh "cmd" "/c" "start" url)
       PlatformID/Unix         (if (RuntimeInformation/IsOSPlatform OSPlatform/OSX)
                                 (shell/sh "open" url)
                                 (shell/sh "xdg-open" url))
       (println "Goto" url "to view portal ui."))))

#?(:clj (defn- random-uuid [] (java.util.UUID/randomUUID)))

(defn url [{:keys [portal server]}]
  (str "http://" (:host server) ":" (:port server) "?" (:session-id portal)))

(defmethod -open :default [{:keys [options] :as args}]
  (let [chrome-bin   (get-chrome-bin)
        chrome-flags (::flags options flags)]
    (if (and (some? chrome-bin)
             (:app options true))
      (apply shell/sh chrome-bin (chrome-flags (url args)))
      (browse (url args)))))

(defmethod -open false [_args])

(defn open [{:keys [portal options] :as args}]
  (let [portal     (or portal (c/make-atom (random-uuid)))
        session-id (:session-id portal)]
    (swap! rt/sessions update-in [session-id :options] merge options)
    (let [options (merge @rt/default-options
                         (get-in @rt/sessions [session-id :options]))]
      (when (or (not (c/open? session-id)) (:always-open options))
        (-open (assoc args :portal portal :options options))))
    portal))
