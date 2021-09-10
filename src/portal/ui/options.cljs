(ns portal.ui.options
  (:require ["react" :as react]
            [portal.ui.state :as state]))

(defonce ^:private options-context (react/createContext nil))

(defn with-options [& children]
  (let [[options set-options!] (react/useState ::loading)]
    (react/useEffect
     (fn []
       (-> (state/invoke `portal.runtime/get-options)
           (.then set-options!)))
     #js [])
    (into [:r> (.-Provider options-context)
           #js {:value options}]
          children)))

(defn use-options [] (react/useContext options-context))
