(ns portal.client.web
  (:require
   [portal.client.common :refer (->send!)]))

(def send! (->send! js/fetch))

(comment
  (send! nil "Hello World")
  (add-tap send!)
  (tap> #?(:cljs {:runtime 'cljs :value "hello web"}))
  (add-tap send!)

  (add-tap (partial send! {:encoding :json}))
  (add-tap (partial send! {:encoding :edn})))
