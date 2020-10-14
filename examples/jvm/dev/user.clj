(ns user
  (:require [example.server :refer [handler]]
            [portal.api :as p]
            [ring.adapter.jetty :refer [run-jetty]]))

(def portal (p/open))

(p/tap)

(def server (atom nil))

(defn go []
  (println "starting server on http://localhost:3000")
  (reset! server (run-jetty #'handler {:port 3000 :join? false})))

(comment
  (tap> :hello)
  (-> @portal first)
  (swap! portal keys))

(go)
