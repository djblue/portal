(ns portal.runtime.client
  (:require #?(:bb   [portal.runtime.client.bb :as c]
               :clj  [portal.runtime.client.jvm :as c]
               :cljs [portal.runtime.client.node :as c])))

(def ops c/ops)
(def make-atom c/make-atom)
