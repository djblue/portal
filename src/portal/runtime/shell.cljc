(ns ^:no-doc portal.runtime.shell
  #?(:clj  (:require [clojure.java.shell :as shell])
     :cljs (:require ["child_process" :as cp])
     :cljr (:require [clojure.clr.shell :as shell])
     :lpy  (:require [basilisp.shell :as shell])))

(defn spawn [bin & args]
  #?(:clj
     (future
       (let [{:keys [exit err out]} (apply shell/sh bin args)]
         (when-not (zero? exit)
           (prn (into [bin] args))
           (println err out))))
     :cljs
     (js/Promise.
      (fn [resolve reject]
        (let [ps (cp/spawn bin (clj->js args))]
          (.on ps "error" reject)
          (.on ps "close" resolve))))
     :cljr
     (future
       (let [{:keys [exit err out]} (apply shell/sh bin args)]
         (when-not (zero? exit)
           (prn (into [bin] args))
           (println err out))))
     :lpy
     (future
       (let [{:keys [exit err out]} (apply shell/sh bin args)]
         (when-not (zero? exit)
           (prn (into [bin] args))
           (println err out))))))

(defn sh [bin & args]
  #?(:clj  (apply shell/sh bin args)
     :cljs (let [result (cp/spawnSync bin (clj->js args))]
             {:exit (.-status result)
              :out  (str (.-stdout result))
              :err  (str (.-stderr result))})
     :cljr (apply shell/sh bin args)
     :lpy  (apply shell/sh bin args)))