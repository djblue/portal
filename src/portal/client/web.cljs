(ns portal.client.web
  (:require
   [portal.client.common :refer (->submit)]))

(def submit (->submit js/fetch))

(comment
  (submit "Hello World")
  (add-tap submit)
  (tap> {:runtime :cljs :value "hello web"})
  (add-tap submit)

  (add-tap (partial submit {:encoding :json}))
  (add-tap (partial submit {:encoding :transit})))
