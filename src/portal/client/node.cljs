(ns portal.client.node
  (:require
   [clojure.string :as str]
   [portal.client.common :refer (->submit)]))

(defn fetch [url options]
  (let [https? (str/starts-with? url "https")
        http   (js/require (str "http" (when https? "s")))]
    (js/Promise.
     (fn [resolve reject]
       (let [req (.request
                  http
                  url
                  (clj->js options)
                  (fn [^js res]
                    (let [body (atom "")]
                      (.on res "data" #(swap! body str %))
                      (.on res "error" reject)
                      (.on res "end" #(resolve
                                       {:status (.-statusCode res)
                                        :body   @body})))))]
         (.write req (:body options))
         (.end req))))))

(def submit (->submit fetch))

(comment
  (submit {:runtime :node :value "hello node"})
  (add-tap submit)
  (tap> {:runtime :node :value "hello node-tap"})
  (add-tap submit))
