(ns tracker
  (:require [ns-tracker.core :as tracker]))

(defonce ^:private running? (atom false))

(defn- check-namespace-changes [track]
  (try
    (let [namespaces (track)]
      (doseq [ns-sym namespaces]
        (require ns-sym :reload)))
    (catch Throwable e (.printStackTrace e)))
  (Thread/sleep 500))

(defn start []
  (when-not @running?
    (reset! running? true)
    (let [track (tracker/ns-tracker ["src" "resources" "test" "dev"])]
      (doto
       (Thread.
        #(while @running?
           (check-namespace-changes track)))
        (.setDaemon true)
        (.start)))))

(defn stop []
  (reset! running? false))
