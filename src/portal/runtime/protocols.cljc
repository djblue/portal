(ns ^:no-doc portal.runtime.protocols
  (:refer-clojure :exclude [send]))

(defprotocol Listener
  (on-open    [listener socket])
  (on-message [listener socket message])
  (on-pong    [listener socket data])
  (on-error   [listener socket throwable])
  (on-close   [listener socket code reason]))

(defprotocol Socket
  (send  [socket message])
  (close [socket code reason]))