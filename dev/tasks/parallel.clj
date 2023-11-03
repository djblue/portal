(ns tasks.parallel
  (:require [org.httpkit.server :as http]
            [portal.runtime.cson :as cson]
            [portal.viewer :as v]
            [tasks.tools :refer [*opts*]]))

(def ^:private ^:dynamic *sessions* nil)

(defn- create-sessions []
  (atom (with-meta [] (v/for
                       {:stdio []
                        :results {}
                        :done (promise)
                        :start (System/currentTimeMillis)}
                        {:start :portal.viewer/date-time
                         :time  :portal.viewer/duration-ms}))))

(defn- new-session [sessions]
  (-> sessions
      (swap! vary-meta update :stdio conj [])
      meta
      :stdio
      count
      dec))

(defn- append-result [sessions id result]
  (let [{:keys [stdio results done]}
        (meta
         (swap!
          sessions
          (fn [sessions]
            (let [m       (meta sessions)
                  results (-> m
                              :results
                              (assoc id result))]
              (with-meta
                sessions
                (assoc m :results results))))))]
    (when (= (count stdio) (count results))
      (let [start (:start (meta @sessions))
            stop  (System/currentTimeMillis)]
        (swap! sessions vary-meta assoc :time (- stop start)))
      (deliver done true))))

(defn- append-stdio [sessions id data]
  (swap!
   sessions
   (fn [sessions]
     (let [m     (meta sessions)
           stdio (-> m
                     :stdio
                     (update id conj data))]
       (with-meta
         (into [] (apply concat stdio))
         (assoc m :stdio stdio))))))

(def ^:dynamic *portal* true)

(defn- with-session* [f & args]
  (let [sessions *sessions*
        id       (new-session sessions)
        server   (http/run-server
                  (fn [request]
                    (append-stdio sessions id {:tag :tap :val (cson/read (slurp (:body request)))})
                    {:status 200})
                  {:port 0 :legacy-return-value? false})
        port     (http/server-port server)
        out      (PrintWriter-on
                  (fn [val]
                    (append-stdio sessions id {:tag :out :val val})) nil)
        err      (PrintWriter-on
                  (fn [val]
                    (append-stdio sessions id {:tag :err :val val})) nil)]
    (try
      (binding [*out*  out
                *err*  err
                *opts* (cond-> *opts* *portal* (assoc-in [:extra-env "PORTAL_PORT"] port))]
        (let [result (apply f args)]
          (append-result sessions id result)
          result))
      (catch Exception e
        (append-result sessions id e)
        (throw e))
      (finally (http/server-stop! server)))))

(defn with-out-data* [f]
  (let [sessions (create-sessions)]
    (binding [*sessions* sessions
              *opts*     (assoc *opts* :session with-session*)]
      (try
        (future (f))
        #_(deref (-> sessions deref meta :done) 60000 ::timeout)
        #_(->> sessions deref meta :results (sort-by first) (map second))
        sessions
        (catch Exception e
          (throw (ex-info (ex-message e) (assoc (ex-data e) :stdio @sessions))))))))

(defmacro with-out-data [& body] `(with-out-data* (fn [] ~@body)))
