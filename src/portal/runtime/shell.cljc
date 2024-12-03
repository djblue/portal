(ns portal.runtime.shell
  {:no-doc true}
  (:require
   #?(:clj [clojure.java.shell :as shell]
      :cljr [clojure.clr.shell :as shell]
      :cljs ["child_process" :as cp])))

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
           (println err out))))))

(defn sh [bin & args]
  #?(:clj  (apply shell/sh bin args)
     :cljs (let [result (cp/spawnSync bin (clj->js args))]
             {:exit (.-status result)
              :out  (str (.-stdout result))
              :err  (str (.-stderr result))})
     :cljr (apply shell/sh bin args)))
