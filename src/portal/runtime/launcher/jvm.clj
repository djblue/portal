(ns portal.runtime.launcher.jvm
  (:require [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [portal.runtime :as rt]
            [portal.runtime.client :as c]
            [portal.runtime.http-socket-server :as http]
            [portal.runtime.server :as server])
  (:import [java.util UUID]))

(defn- random-uuid [] (UUID/randomUUID))

(defn- get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"]
   (s/split (System/getenv "PATH") #":")))

(defn- find-bin [files]
  (some
   identity
   (for [path (get-paths) file files]
     (let [f (io/file path file)]
       (when (and (.exists f) (.canExecute f))
         (.getAbsolutePath f))))))

(defn- get-chrome-bin []
  (find-bin #{"chrome" "google-chrome-stable" "chromium" "Google Chrome"}))

(defonce ^:private server (atom nil))

(defn- chrome-flags [url]
  ["--incognito"
   "--disable-features=TranslateUI"
   "--no-first-run"
   (str "--app=" url)])

(defn open [options]
  (swap! rt/state merge {:portal/open? true} options)
  (when (nil? @server)
    (reset!
     server
     (http/start #'server/handler)))
  (let [session-id (random-uuid)
        url (str "http://localhost:" (-> @server :port) "?" session-id)]
    (if-let [bin (get-chrome-bin)]
      (let [flags (chrome-flags url)]
        (future (apply sh bin flags)))
      (browse-url url))
    (c/make-atom session-id)))

(defn close []
  (swap! rt/state assoc :portal/open? false)
  (http/stop @server)
  (reset! server nil))
