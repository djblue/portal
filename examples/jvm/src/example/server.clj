(ns example.server)

(defn handler [req]
  (tap> req)
  {:status 200 :body "hello, world"})
