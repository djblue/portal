(ns cljs.user
  (:require [clojure.core.protocols :refer [Datafiable]]
            [clojure.datafy :refer [datafy]]
            [examples.data :refer [data]]
            [portal.web :as p]))

(p/tap)

(defn swap-dev []
  (set! js/portal.runtime.web.launcher.code_url
        (str js/window.location.origin "/main.js")))

(extend-protocol Datafiable
  js/Promise
  (datafy [this] (.then this identity))

  js/Error
  (datafy [this]
    {:name     (.-name this)
     :message  (.-message this)
     :stack    (.-stack this)}))

(comment
  (swap-dev)

  (def portal (p/open))
  (p/tap)
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

  (tap> data))
