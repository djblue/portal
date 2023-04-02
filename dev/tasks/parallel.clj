(ns tasks.parallel
  (:require [org.httpkit.server :as http]
            [portal.runtime.cson :as cson]
            [tasks.tools :refer [*opts*]]))

(def ^:private ^:dynamic *sessions* nil)

(defn- create-sessions []
  (atom (with-meta [] {:stdio [] :results {} :done (promise)})))

(defn- new-session [sessions]
  (-> sessions
      (swap! vary-meta update :stdio conj [])
      meta
      :stdio
      count
      dec))

(defn- append-result [sessions id result]
  (let [sessions
        (swap!
         sessions
         (fn [sessions]
           (let [m       (meta sessions)
                 results (-> m
                             :results
                             (assoc id result))]
             (with-meta
               sessions
               (assoc m :results results)))))
        {:keys [stdio results done]} (meta sessions)]
    (when (= (count stdio) (count results))
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
                *opts* (assoc-in *opts* [:extra-env "PORTAL_PORT"] port)]
        (let [result (apply f args)]
          (append-result sessions id result)
          result))
      (catch Exception e
        (append-result sessions id e)
        (throw e))
      (finally (http/server-stop! server)))))

(defn with-out-data* [f]
  (let [sessions (create-sessions)]
    (try (binding [*sessions* sessions
                   *opts*     (assoc *opts* :session with-session*)]
           (f))
         (catch Exception e
           (tap> e)))
    (deref (-> sessions deref meta :done) 60000 ::timeout)
    (->> sessions deref meta :results (sort-by first) (map second))
    @sessions))

(defmacro with-out-data [& body] `(with-out-data* (fn [] ~@body)))
