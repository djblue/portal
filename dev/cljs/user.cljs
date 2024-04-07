(ns cljs.user)

(comment
  (require '[portal.api :as p])
  (add-tap #'p/submit)
  (remove-tap #'p/submit)

  (defn async-submit [value]
    (if-not (instance? js/Promise value)
      (p/submit value)
      (-> value
          (.then p/submit)
          (.catch p/submit))))

  (add-tap #'async-submit)
  (tap> :hi)

  (p/clear)
  (p/close)
  (p/docs)

  (p/open)

  (require '[examples.data :refer [data]])
  (dotimes [_i 25] (tap> data))

  comment)