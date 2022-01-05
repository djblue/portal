(ns portal.setup
  (:require [clojure.core.protocols :refer [Datafiable]]
            [examples.data :refer [data]]
            [portal.client.web :as client]
            [portal.console :as log]
            [portal.ui.state :as state]
            [portal.web :as p]))

(def submit (partial client/submit {:port js/window.location.port}))

(add-tap #'submit)
(add-tap #'p/submit)

(extend-protocol Datafiable
  js/Promise
  (datafy [this] (.then this identity))

  js/Error
  (datafy [this]
    {:name     (.-name this)
     :message  (.-message this)
     :stack    (.-stack this)}))

(comment
  (def portal (p/open))
  (def portal (p/open {:mode :dev :value state/state}))

  (add-tap #'p/submit)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (doseq [i (range 100)] (tap> [::index i]))
  (p/clear)
  (p/close)

  (-> @portal)
  (tap> portal)
  (swap! portal * 1000)
  (reset! portal 1)

  (tap> (js/Error. "hi"))
  (tap> #js [1 2 3 4 5])
  (tap> (js/Promise.resolve 123))

  (tap> (with-meta (range) {:hello :world}))
  (tap> data)

  (do (log/trace ::trace)
      (log/debug ::debug)
      (log/info  ::info)
      (log/warn  ::warn)
      (log/error ::error)))
