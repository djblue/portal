(ns portal.runtime.browser
  #?(:clj  (:require [cheshire.core :as json]
                     [clojure.java.browse :refer [browse-url]]
                     [clojure.java.shell :as shell]
                     [portal.runtime :as rt]
                     [portal.runtime.fs :as fs]
                     [portal.runtime.jvm.client :as c])
     :cljs (:require ["child_process" :as cp]
                     [portal.runtime :as rt]
                     [portal.runtime.fs :as fs]
                     [portal.runtime.node.client :as c])))

(defn- get-chrome-bin []
  (fs/find-bin
   (concat
    ["/Applications/Google Chrome.app/Contents/MacOS"
     "/mnt/c/Program Files (x86)/Google/Chrome/Application"]
    (fs/paths))
   ["chrome" "chrome.exe" "google-chrome-stable" "chromium" "Google Chrome"]))

(defn- get-app-id-profile-osx [app-name]
  (let [info (fs/join
              (fs/home)
              "Applications/Chrome Apps.localized/"
              (str app-name ".app")
              "Contents/Info.plist")]
    (when (fs/exists info)
      {:app-id
       (->> info
            fs/slurp
            (re-find #"com\.google\.Chrome\.app\.([^<]+)")
            second)})))

(defn- parse-json [string]
  #?(:clj (json/parse-string string) :cljs (js->clj (.parse js/JSON string))))

(defn- get-app-id-from-pref-file [path app-name]
  (when (fs/exists path)
    (some
     (fn [[id extension]]
       (let [name (get-in extension ["manifest" "name"] "")]
         (when (= app-name name) id)))
     (get-in
      (parse-json (fs/slurp path))
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

(defn- sh [bin & args]
  #?(:clj
     (future
       (let [{:keys [exit err out]} (apply shell/sh bin args)]
         (when-not (zero? exit)
           (println "Unable to open chrome:")
           (prn (into [bin] args))
           (println err out))))
     :cljs
     (js/Promise.
      (fn [resolve reject]
        (let [ps (cp/spawn bin (clj->js args))]
          (.on ps "error" reject)
          (.on ps "close" resolve))))))

(defn- browse [url]
  #?(:clj
     (try (browse-url url)
          (catch Exception _e
            (println "Goto" url "to view portal ui.")))
     :cljs
     (case (.-platform js/process)
       ("android" "linux") (sh "xdg-open" url)
       "darwin"            (sh "open" url)
       "win32"             (sh "cmd" "/c" "start" url)
       (println "Goto" url "to view portal ui."))))

#?(:clj (defn- random-uuid [] (java.util.UUID/randomUUID)))

(defn open [{:keys [portal options server]}]
  (let [{:keys [host port]} server
        portal     (or portal (c/make-atom (random-uuid)))
        session-id (:session-id portal)
        url        (str "http://" host ":" port "?" session-id)
        chrome-bin (get-chrome-bin)]
    (swap! rt/sessions assoc-in [session-id :options] options)
    (when-not (c/open? session-id)
      (if (and (some? chrome-bin)
               (:portal.launcher/app options true))
        (apply sh chrome-bin (flags url))
        (browse url)))
    portal))
