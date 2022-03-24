(ns portal.client.planck
  (:require [planck.http :as http]
            [portal.client.common :refer (->submit)]))

(def ^:private http-methods
  {"GET"   http/get
   "HEAD"  http/head
   "PATCH" http/patch
   "POST"  http/post
   "PUT"   http/put})

(defn- fetch [url options]
  (let [f (get http-methods (:method options))]
    (f url options)))

(def ^{:see-also ["portal.api/submit"]}
  submit (->submit fetch))

(comment
  (require '[portal.client.planck :as c] :reload)
  (c/submit {:port 7777} ::hello)
  (add-tap (partial c/submit {:port 7777}))
  (tap> ::hello))
