(ns portal.shadow.remote
  (:require [portal.api :as p]))

(defn hook
  {:shadow.build/stage :compile-prepare}
  ([build-state]
   (hook build-state nil))
  ([build-state options]
   (cond-> build-state
     (= (:shadow.build/mode build-state) :dev)
     (update-in
      [:compiler-options :closure-defines]
      merge
      (let [{:keys [port host]} (p/start options)]
        {`port port `host host})))))
