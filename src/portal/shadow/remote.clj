(ns portal.shadow.remote
  (:require [portal.api :as p]))

(defn hook
  {:shadow.build/stage :compile-prepare}
  [build-state]
  (assoc-in
   build-state
   [:compiler-options :closure-defines `port]
   (:port (p/start nil))))
