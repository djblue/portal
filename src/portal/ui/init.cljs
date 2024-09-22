(ns portal.ui.init
  (:require [cljs.analyzer :as ana]
            [portal.ui.boot :as boot]
            [portal.ui.load :as load]))

(def ^:private modules '#{react react-dom})

(defn node-module-dep? [module]
  (or (string? module) (contains? modules module)))

(set! ana/node-module-dep? node-module-dep?)

(defn inline [resource-name]
  (:source (load/load-fn-sync {:path resource-name :name resource-name :resource true})))

(set! (.-INLINE js/window)  inline)

(defn init []
  (.time js/console "app-boot")
  (.catch
   (boot/eval-str
    "(require 'portal.ui.core)
   (require 'portal.ui.repl.boot.eval)
  (portal.ui.core/main!)
  (.timeEnd js/console \"app-boot\")
  (prn :load-time @portal.ui.load/load-time)
  (prn :require-time @portal.ui.load/require-time)
  "
    {:load load/load-fn
     :cache-source load/cache-source})
   (fn [error]
     (.error js/console (pr-str error)))))

(set! (.-INIT js/window) init)
(init)