(ns user
  (:require [examples.data :refer [data]]
            [portal.web :as p]
            [clojure.datafy :refer [datafy]]
            [clojure.core.protocols :refer [Datafiable]]))

(comment
  (p/open)
  (p/tap)
  (tap> [{:hello :world :old-key 123} {:hello :youtube :new-key 123}])
  (p/clear)
  (p/close)

  (extend-protocol Datafiable
    js/Promise
    (datafy [this] (.then this identity))

    js/Error
    (datafy [this]
      {:name     (.-name this)
       :message  (.-message this)
       :stack    (.-stack this)}))

  (tap> (js/Error. "hi"))
  (tap> #js [1 2 3 4 5])
  (tap> (js/Promise.resolve 123))

  (tap> data))
