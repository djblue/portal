(ns portal.runtime.server
  (:require #?(:clj  [portal.runtime.server.jvm :as s]
               :cljs [portal.runtime.server.node :as s])))

(def handler s/handler)
