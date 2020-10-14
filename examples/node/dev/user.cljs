(ns cljs.user
  (:require [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy nav]]
            [example.server :refer [handler]]
            [portal.api :as p]
            ["http" :as http]))

(def portal (p/open))

(p/tap)

(defn js->clj+
  "For cases when built-in js->clj doesn't work. Source: https://stackoverflow.com/a/32583549/4839573"
  [x]
  (into {} (for [k (js-keys x)] [(keyword k) (aget x k)])))

(extend-protocol Datafiable
  http/IncomingMessage
  (datafy [this] (js->clj+ this)))

(def server (atom nil))

(defn go []
  (println "starting server on http://localhost:3000")
  (reset! server (http/createServer #(handler %1 %2)))
  (.listen @server 3000))

(go)
