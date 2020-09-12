(ns portal.runtime.launcher
  (:require #?(:clj  [portal.runtime.launcher.jvm :as l]
               :cljs [portal.runtime.launcher.node :as l])))

(def open  l/open)
(def close l/close)
