(ns portal.http-socket-server
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.net ServerSocket Socket]
           [java.io BufferedReader]))

(defn- parse-headers [headers]
  (let [headers
        (->> headers
             (map #(string/split % #": "))
             (into {}))]
    (if-not (contains? headers "Content-Length")
      headers
      (update headers "Content-Length" #(Integer/parseInt %)))))

(defn- parse-request [^BufferedReader in]
  (let [[req-line & headers]
        (loop [headers []]
          (let [line (.readLine in)]
            (if (string/blank? line)
              headers
              (recur (conj headers line)))))
        [method path] (string/split req-line #" ")
        headers (parse-headers headers)]
    {:method  method
     :uri     path
     :headers headers
     :body (when-let [content-length (get headers "Content-Length")]
             (let [buffer (char-array content-length)]
               (.read in buffer 0 content-length)
               (String. buffer)))}))

(def messages
  {200 "OK"
   404 "Not Found"
   500 "Internal Server Error"})

(defn content-length [^String s] (count (.getBytes s)))

(defn- format-response ^String [response]
  (let [{:keys [status body headers] :or {status 404}} response
        headers (if (empty? body)
                  headers
                  (assoc headers "Content-Length" (content-length body)))]
    (str "HTTP/1.1 " status " " (get messages status) "\r\n"
         (string/join
          (map (fn [[k v]] (str k ": " v "\r\n")) headers))
         (when body "\r\n")
         body)))

(defn- start-worker-thread [^Socket client-socket request-handler]
  (future
    (with-open [in  (io/reader (.getInputStream client-socket))
                out (io/writer (.getOutputStream client-socket))]
      (let [closed?  (promise)
            request  (assoc (parse-request in) :closed? closed?)
            _        (future
                       (try
                         (.read in)
                         (finally
                           (deliver closed? true))))
            response (try
                       (request-handler request)
                       (catch Exception _e {:status 500}))]
        (when-not (realized? closed?)
          (.write out (format-response response))
          (.flush out))))))

(defn start [handler]
  (let [server-socket (ServerSocket. 0)
        port          (.getLocalPort server-socket)]

    {:port port
     :server-socket server-socket
     :future (future
               (while (not (.isClosed server-socket))
                 (when-let [client-socket
                            (try (.accept server-socket)
                                 (catch Exception _e))]
                   (start-worker-thread client-socket handler))))}))

(defn wait [server] @(:future server))

(defn stop [server]
  (.close ^ServerSocket (:server-socket server)))

