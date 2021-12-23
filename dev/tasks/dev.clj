(ns tasks.dev
  (:require [tasks.build :refer [build]]
            [tasks.tools :refer [clj]]))

(defn dev
  "Start dev server."
  []
  (build)
  (clj "-M:dev:cider:cljs:shadow" :watch :pwa :client :vs-code))

(defn -main [] (dev))
