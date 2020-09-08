(ns cljs.user
  (:require [examples.data :refer [data]]
            [portal.web :as p]
            [clojure.datafy :refer [datafy]]
            [clojure.core.protocols :refer [Datafiable]]))

(p/tap)

(extend-protocol Datafiable
  js/Promise
  (datafy [this] (.then this identity))

  js/Error
  (datafy [this]
    {:name     (.-name this)
     :message  (.-message this)
     :stack    (.-stack this)}))

(comment
  (set! js/portal.web.code_url (str js/window.location.origin "/main.js"))

  (p/open)
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (p/clear)
  (p/close)

  (tap> (js/Error. "hi"))
  (tap> #js [1 2 3 4 5])
  (tap> (js/Promise.resolve 123))

  (tap> data))
