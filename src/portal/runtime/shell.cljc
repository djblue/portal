(ns ^:no-doc portal.runtime.shell
  #?(:clj  (:require [clojure.java.shell :as shell])
     :cljs (:require ["child_process" :as cp])
     :cljr (:require [clojure.clr.shell :as shell])))

(defn sh [bin & args]
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
           (println err out))))))
