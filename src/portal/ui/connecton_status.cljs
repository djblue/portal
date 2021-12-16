(ns ^:no-doc portal.ui.connecton-status
  (:require ["react" :as react]
            [portal.ui.state :as state]))

(defonce ^:private context (react/createContext true))

(defn- use-interval [f milliseconds]
  (react/useEffect
   (fn []
     (when (fn? f)
       (let [interval (js/setInterval f milliseconds)]
         (fn [] (js/clearInterval interval)))))
   #js [f]))

(defn- use-connected []
  (let [[{:keys [errors connected?]} set-status!]
        (react/useState {:connected? true :errors 0})]
    (use-interval
     (if (> errors 5)
       (.error js/console "Unable to establish connection to runtime.")
       (fn []
         (-> (state/invoke 'portal.runtime/ping)
             (.then  #(set-status! {:connected? true  :errors 0}))
             (.catch #(set-status! {:connected? false :errors (inc errors)})))))
     5000)
    connected?))

(defn use-status [] (react/useContext context))

(defn with-status [& children]
  (let [connected? (use-connected)]
    (into [:r> (.-Provider context) #js {:value connected?}] children)))
