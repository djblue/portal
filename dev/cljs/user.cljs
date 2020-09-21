(ns cljs.user
  (:require [examples.data :refer [data]]
            [clojure.datafy :refer [datafy]]
            [clojure.core.protocols :refer [Datafiable]]))

(defn swap-dev []
  (set! js/portal.runtime.launcher.web.code_url
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
  (require '[portal.api :as p] :reload)
  (require '[portal.web :as p] :reload)
  (p/tap)

  (swap-dev)

  (def portal (portal.api/open))
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
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
