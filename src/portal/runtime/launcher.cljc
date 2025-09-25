(ns ^:no-doc portal.runtime.launcher
  (:require #?(:clj  [portal.client.jvm :as client]
               :cljs [portal.client.node :as client]
               :cljr [portal.client.clr :as client]
               :lpy  [portal.client.py :as client])
            #?(:cljs    [portal.async :as a]
               :default [portal.sync :as a])
            [clojure.edn :as edn]
            [portal.runtime.browser :as browser]
            [portal.runtime.fs :as fs]
            [portal.runtime.shell :refer [spawn]]))

(defn- get-workspace-folder []
  #?(:cljs
     (try
       (let [vscode (js/require "vscode")
             ^js uri (-> vscode .-workspace .-workspaceFolders (aget 0) .-uri)
             fs-path (.-fsPath uri)]
         (if-not (undefined? fs-path) fs-path (.-path uri)))
       (catch :default _e))))

(defn- get-search-paths [base-path]
  (->> base-path (iterate fs/dirname) (take-while some?)))

(defn get-config [{:keys [options config-file]}]
  (let [search-paths (concat (get-search-paths (fs/cwd))
                             (some-> (get-workspace-folder) get-search-paths))]
    (or (some
         (fn [parent]
           (some-> parent
                   (fs/join ".portal" config-file)
                   fs/exists
                   fs/slurp
                   edn/read-string
                   (merge (when-let [config (:launcher-config options)]
                            config))))
         search-paths)
        (throw
         (ex-info
          (str "No config file found: " config-file)
          {:options      options
           :config-file  config-file
           :search-paths search-paths})))))

(defn- localhost
  "https://github.com/nodejs/node/issues/40537"
  [host]
  (if (= "localhost" host) "127.0.0.1" host))

(defn- remote-open [{:keys [portal options server] :as args}]
  (a/let [config (get-config args)
          {:keys [status error] :as response}
          (client/request
           {:url     (str "http://" (localhost (:host config)) ":" (:port config) "/open")
            :method  :post
            :headers {"content-type" "application/edn"}
            :body    (pr-str {:portal  (into {} portal)
                              :options (select-keys options [:window-title])
                              :server  (select-keys server [:host :port])})})]
    (when (or error (not= status 200))
      (throw (ex-info "Unable to open extension"
                      {:options  options
                       :config   config
                       :response (select-keys response [:body :headers :status])})))))

(defmethod browser/-open :intellij [args]
  (try
    (remote-open (assoc args :config-file "intellij.edn"))
    (catch #?(:cljs js/Error :default Exception) e
      (throw
       (ex-info
        (str
         (ex-message e)
         ": Please ensure extension is installed and Portal tab is open.")
        (ex-data e))))))

(defmethod browser/-open :vs-code  [args] (remote-open (assoc args :config-file "vs-code.edn")))
(defmethod browser/-open :electron [args] (remote-open (assoc args :config-file "electron.edn")))

(defmethod browser/-open :emacs [{:keys [portal server]}]
  (let [url (str "http://" (:host server) ":" (:port server) "?" (:session-id portal))]
    (spawn "emacsclient" "--no-wait" "--eval"
           (str "(xwidget-webkit-browse-url " (pr-str url) ")"))))

(defmethod browser/-open :auto [args]
  (browser/-open
   (assoc-in args [:options :launcher]
             (cond
               (fs/exists ".portal/vs-code.edn") :vs-code
               (fs/exists ".portal/intellij.edn") :intellij))))