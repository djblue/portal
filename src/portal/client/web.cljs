(ns portal.client.web
  (:require
   [portal.client.common :refer (->submit)]))

(defn- fetch [url options]
  (js/fetch url (clj->js options)))

(def submit (->submit fetch))

(comment
  (submit "Hello World")
  (add-tap submit)
  (tap> {:runtime :cljs :value "hello web"})
  (add-tap submit)

  (add-tap (partial submit {:encoding :json}))
  (add-tap (partial submit {:encoding :transit})))
