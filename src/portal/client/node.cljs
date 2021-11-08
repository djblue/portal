(ns portal.client.node
  (:require
   [clojure.string :as str]
   [portal.client.common :refer (->send!)]))

(defn fetch [url options]
  (let [https? (str/starts-with? url "https")
        http (js/require (str "http" (when https? "s")))]
    (js/Promise.
     (fn [resolve reject]
       (let [req (.request
                  http
                  url
                  options
                  (fn [res]
                    (let [body (atom "")]
                      (.on res "data" #(swap! body str %))
                      (.on res "error" reject)
                      (.on res "end" #(resolve @body)))))]
         (.write req (.-body options))
         (.end req))))))

(def send! (->send! fetch))

(comment
  (send! nil {:runtime 'node :value "hello node"})
  (add-tap send!)
  (tap> {:runtime 'node :value "hello node-tap"})
  (add-tap send!))
