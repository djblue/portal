(ns portal.shadow.remote
  (:require [portal.api :as p]))

(defn hook
  {:shadow.build/stage :compile-prepare}
  [build-state]
  (cond-> build-state
    (= (:shadow.build/mode build-state) :dev)
    (assoc-in
     [:compiler-options :closure-defines `port]
     (:port (p/start nil)))))
