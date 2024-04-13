(ns start
  (:require [nbb.nrepl-server :as n]
            [nbb.repl :as r]
            [portal.api :as p]))

(defn async-submit [value]
  (if-not (instance? js/Promise value)
    (p/submit value)
    (-> value
        (.then p/submit)
        (.catch p/submit))))

(defn -main []
  (add-tap #'async-submit)
  (n/start-server! {:port 1337})
  (p/open {:launcher :vs-code})
  (r/repl))

(comment
  (n/stop-server!)
  (require '[portal.console :as console])
  (console/log :hi))
