(ns portal.main
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [portal.server :as server]
            [portal.runtime :as rt])
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

;(defonce state (atom nil))
(defonce server (atom nil))

(defn open-inspector [value]
  (swap! rt/state assoc :portal/open? true)
  (rt/update-value value)
  (when (nil? @server)
    (reset!
     server
     (server/start #'server/handler)))
  (sh (get-chrome-bin)
      "--incognito"
      "--disable-features=TranslateUI"
      "--no-first-run"
      (str "--app=http://localhost:" (-> @server meta :local-port))))

(defn close-inspector []
  (swap! rt/state assoc :portal/open? false)
  (server/stop @server)
  (reset! server nil))

(defn inspect [v]
  (.start (Thread. #(open-inspector v)))
  v)

(defn -main [& args]
  (let [[input-format] args
        in (case input-format
             "json"  (-> *in* io/reader (json/read :key-fn keyword))
             "edn"   (-> *in* io/reader read-edn))]
    (open-inspector in)
    (shutdown-agents)))
