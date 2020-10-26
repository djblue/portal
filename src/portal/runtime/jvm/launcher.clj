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

(defn open [options]
  (swap! rt/state merge {:portal/open? true} options)
  (let [session-id (random-uuid)
        {:keys [host port]} (or @server (start nil))
        url (str "http://" host ":" port "?" session-id)]
    (if-let [bin (get-chrome-bin)]
      (let [flags (chrome-flags url)]
        (future (apply sh bin flags)))
      (browse-url url))
    (c/make-atom session-id)))

(defn close []
  (swap! rt/state assoc :portal/open? false)
  (some-> server deref :http-server http/server-stop!)
  (reset! server nil))
