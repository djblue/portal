(ns ^:no-doc ^:no-check portal.runtime.clr.launcher
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.clr.client :as c]
            [portal.runtime.clr.server :as server]
            [portal.runtime.fs :as fs]
            [portal.runtime.shell :refer [spawn]])
  (:import (System.Collections.Generic KeyValuePair)
           (System.Net HttpListener HttpListenerContext)
           (System.Net.Http
            HttpClient
            HttpCompletionOption
            HttpMethod
            HttpRequestMessage
            HttpResponseMessage
            StringContent)
           (System.Net.Http.Headers HttpResponseHeaders)
           (System.Text Encoding)))

(defn- get-search-paths []
  (->> (fs/cwd) (iterate fs/dirname) (take-while some?)))

(defn- get-config [{:keys [options config-file]}]
  (let [search-paths (get-search-paths)]
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

(def ^:private client (HttpClient.))

(defn- ->resposne-headers [^HttpResponseMessage response]
  (let [^HttpResponseHeaders headers         (.Headers response)
        ^HttpResponseHeaders content-headers (.Headers (.Content response))]
    (reduce
     (fn [out ^KeyValuePair x]
       (let [k (.Key x) v (.Value x)]
         (assoc out
                (keyword (str/lower-case k))
                (cond-> v (= 1 (count v)) first))))
     {}
     (concat
      (iterator-seq (.GetEnumerator headers))
      (iterator-seq (.GetEnumerator content-headers))))))

(defn- ->response [^HttpResponseMessage response]
  {:status  (int (.StatusCode response))
   :headers (->resposne-headers response)
   :body    (.Content response)})

(defn- fetch [url
              {:keys [method body encoding]
               :or   {encoding :edn
                      method :get}}]
  (let [request  (HttpRequestMessage.
                  (case method
                    :get HttpMethod/Get
                    :post HttpMethod/Post
                    :put HttpMethod/Put
                    :delete HttpMethod/Delete)
                  url)]
    (when body
      (set! (.-Content request)
            (StringContent.
             body
             Encoding/UTF8
             (case encoding
               :edn "application/edn"
               :cson "application/cson"))))
    (->response (.Send ^HttpClient client request HttpCompletionOption/ResponseContentRead))))

(defn- remote-open [{:keys [portal options server] :as args}]
  (let [config (get-config args)
        url  (str "http://" (:host config) ":" (:port config) "/open")
        opts {:method :post
              :body   (pr-str {:portal  (into {} portal)
                               :options (select-keys options [:window-title])
                               :server  (select-keys server [:host :port])})}
        {:keys [status error] :as response} (fetch url opts)]
    (when (or error (not= status 200))
      (throw (ex-info "Unable to open extension"
                      {:options  options
                       :config   config
                       :response (select-keys response [:body :headers :status])}
                      error)))))

(defmethod browser/-open :intellij [args]
  (try
    (remote-open (assoc args :config-file "intellij.edn"))
    (catch Exception e
      (throw
       (ex-info
        (str
         (ex-message e)
         ": Please ensure extension is installed and Portal tab is open.")
        (ex-data e))))))

(defmethod browser/-open :vs-code  [args] (remote-open (assoc args :config-file "vs-code.edn")))

(defmethod browser/-open :emacs [{:keys [portal server]}]
  (let [url (str "http://" (:host server) ":" (:port server) "?" (:session-id portal))]
    (spawn "emacsclient" "--no-wait" "--eval"
           (str "(xwidget-webkit-browse-url " (pr-str url) ")"))))

(defmethod browser/-open :electron [args] (remote-open (assoc args :config-file "electron.edn")))

(defmethod browser/-open :auto [args]
  (browser/-open
   (assoc-in args [:options :launcher]
             (cond
               (fs/exists ".portal/vs-code.edn") :vs-code
               (fs/exists ".portal/intellij.edn") :intellij))))

(defn- read-request [^HttpListenerContext context]
  (let [request            (.Request context)
        method             (.HttpMethod request)
        headers            (.Headers request)
        [uri query-string] (str/split (.RawUrl request) #"\?")]
    {:scheme         :http
     :request-method (keyword (str/lower-case method))
     :uri            uri
     :query-string   query-string
     :headers        (reduce
                      (fn [out ^String header]
                        (let [values (.GetValues headers header)]
                          (assoc out
                                 (str/lower-case header)
                                 (str/join ", " values))))
                      {}
                      (.AllKeys headers))
     :websocket?     (.IsWebSocketRequest request)
     :body           (.InputStream request)
     :context        context}))

(defn- write-response [^HttpListenerContext context m]
  (let [response (.Response context)
        headers  (.Headers response)]
    (set! (.-StatusCode response) (:status m 200))
    (doseq [[k v] (:headers m)]
      (.Add headers ^String k ^String v))
    (when-let [body (:body m)]
      (let [bytes  (.GetBytes Encoding/UTF8 ^bytes body)
            length (.Length bytes)]
        (set! (.-ContentLength64 response) length)
        (.Write (.OutputStream response) bytes 0 length)))
    (.Close response)))

(defonce ^:private server (atom nil))

(defn start [options]
  (let [options (merge @rt/default-options options)]
    (or @server
        (let [{:keys [port host]
               :or {port 8001 host "localhost"}} options
              http-server (HttpListener.)]
          (.Add (.Prefixes http-server)
                (str "http://" host ":" port "/"))
          (.Start http-server)
          (reset!
           server
           {:http-server http-server
            :future
            (future
              (while (.IsListening http-server)
                (let [context (.GetContext http-server)]
                  (future
                    (try
                      (let [request-map  (read-request context)
                            response-map (server/handler request-map)]
                        (when-not (:websocket? request-map)
                          (write-response context response-map)))
                      (catch Exception e
                        (write-response context {:status 500 :body (pr-str e)})))))))
            :port port
            :host host})))))

(defn stop []
  (when-let [^HttpListener server (:http-server @server)]
    (.Stop server))
  (reset! server nil))

(defn open
  ([options]
   (open nil options))
  ([portal options]
   (let [server (start options)]
     (browser/open {:portal portal :options options :server server}))))

(defn clear [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/clear})
    (c/request (:session-id portal) {:op :portal.rpc/clear}))
  (rt/cleanup-sessions))

(defn close [portal]
  (if (= portal :all)
    (c/request {:op :portal.rpc/close})
    (c/request (:session-id portal) {:op :portal.rpc/close}))
  (rt/close-session (:session-id portal))
  (rt/cleanup-sessions))

(defn eval-str [portal msg]
  (let [response (if (= portal :all)
                   (c/request (assoc msg :op :portal.rpc/eval-str))
                   (c/request (:session-id portal)
                              (assoc msg :op :portal.rpc/eval-str)))]
    (if-not (:error response)
      response
      (throw (ex-info (:message response) response)))))

(defn sessions []
  (for [session-id (rt/active-sessions)] (c/make-atom session-id)))

(defn url [portal]
  (browser/url {:portal portal :server @server}))

(reset! rt/request c/request)
