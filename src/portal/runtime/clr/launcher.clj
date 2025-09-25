(ns ^:no-doc ^:no-check portal.runtime.clr.launcher
  (:require [clojure.string :as str]
            [portal.runtime :as rt]
            [portal.runtime.browser :as browser]
            [portal.runtime.clr.client :as c]
            [portal.runtime.clr.server :as server])
  (:import (System.Net HttpListener HttpListenerContext)
           (System.Text Encoding)))

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
