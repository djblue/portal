(ns portal.main
  (:require [clojure.java.browse :refer [browse-url]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [portal.server :as server]
            [portal.runtime :as rt]
            [portal.runtime.transit :as t])
  (:import [java.io PushbackReader]))

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

(defn open-inspector []
  (swap! rt/state assoc :portal/open? true)
  (when (nil? @server)
    (reset!
     server
     (server/start #'server/handler)))
  (let [url (str "http://localhost:" (-> @server meta :local-port))]
    (if-let [bin (get-chrome-bin)]
      (future
        (sh bin
            "--incognito"
            "--disable-features=TranslateUI"
            "--no-first-run"
            (str "--app=" url)))
      (browse-url url))))

(defn close-inspector []
  (swap! rt/state assoc :portal/open? false)
  (server/stop @server)
  (reset! server nil))

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"     (-> System/in io/reader (json/read :key-fn keyword))
             "edn"      (-> System/in io/reader read-edn)
             "transit"  (-> System/in t/json-stream->edn))]
    (rt/update-value in)
    (open-inspector)
    (shutdown-agents)))
