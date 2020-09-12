(ns portal.main
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [portal.http-socket-server :as http]
            [portal.runtime :as rt]
            [portal.runtime.client :as c]
            [portal.runtime.server :as server]
            [portal.runtime.transit :as t])
  (:import [java.io PushbackReader]
           [java.util UUID]))

(def random-uuid #(UUID/randomUUID))

(defn get-paths []
  (concat
   ["/Applications/Google Chrome.app/Contents/MacOS"]
   (s/split (System/getenv "PATH") #":")))

(defn find-bin [files]
  (some
   identity
   (for [path (get-paths) file files]
     (let [f (io/file path file)]
       (when (and (.exists f) (.canExecute f))
         (.getAbsolutePath f))))))

(defn get-chrome-bin []
  (find-bin #{"chrome" "google-chrome-stable" "chromium" "Google Chrome"}))

(defn read-edn [reader]
  (with-open [in (PushbackReader. reader)] (edn/read in)))

(defonce server (atom nil))

(defn chrome-flags [url]
  ["--incognito"
   "--disable-features=TranslateUI"
   "--no-first-run"
   (str "--app=" url)])

(defn open-inspector [options]
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

(defn close-inspector []
  (swap! rt/state assoc :portal/open? false)
  (http/stop @server)
  (reset! server nil))

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"     (-> System/in io/reader (json/parse-stream true))
             "edn"      (-> System/in io/reader read-edn)
             "transit"  (-> System/in t/json-stream->edn))]
    (rt/update-value in)
    (http/wait (open-inspector nil))
    (shutdown-agents)))
