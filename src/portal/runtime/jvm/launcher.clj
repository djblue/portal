(ns portal.runtime.jvm.launcher
  (:require [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [org.httpkit.server :as http]
            [portal.runtime :as rt]
            [portal.runtime.jvm.client :as c]
            [portal.runtime.jvm.server :as server])
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

(defn- ensure-server [handler port]
  (when (nil? @server)
    (reset!
     server
     (http/run-server handler
                      {:port port
                       :max-ws (* 1024 1024 1024)
                       :legacy-return-value? false}))))

(defn open [options]
  (swap! rt/state merge {:portal/open? true} options)
  (let [{:portal.launcher/keys [port host]} options
        session-id (random-uuid)
        _  (ensure-server #'server/handler (or port 0))
        url (str "http://" (or host "localhost") ":" (http/server-port @server) "?" session-id)]
    (if-let [bin (get-chrome-bin)]
      (let [flags (chrome-flags url)]
        (future (apply sh bin flags)))
      (browse-url url))
    (c/make-atom session-id)))

(defn close []
  (swap! rt/state assoc :portal/open? false)
  (http/server-stop! @server)
  (reset! server nil))
