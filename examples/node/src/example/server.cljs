(ns example.server)

(defn handler [req res]
  (tap> req)
  (.end res "hello, world"))
