(ns portal.shadow.remote
  (:require [portal.api :as p]))

(defn hook
  {:shadow.build/stage :compile-prepare}
  ([build-state]
   (hook build-state nil))
  ([build-state options]
   (let [{:keys [port host]} (p/start options)]
     (cond-> build-state
       (= (:shadow.build/mode build-state) :dev)
       (update-in
        [:compiler-options :closure-defines]
        assoc
        `port port
        `host host)))))
