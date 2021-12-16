(ns portal.ui.options
  (:require ["react" :as react]
            [clojure.edn :as edn]
            [portal.ui.state :as state]
            [reagent.core :as r]))

(defn- get-extension-options []
  (when-let [options (.getItem js/sessionStorage "PORTAL_EXTENSION_OPTIONS")]
    (edn/read-string options)))

(defonce ^:private extension-options (r/atom (get-extension-options)))

(defn ^:export ^:no-doc patch
  "Function for extensions to patch options after init."
  [edn-string]
  (reset! extension-options (edn/read-string edn-string)))

(defonce ^:private options-context (react/createContext nil))

(defn with-options [& children]
  (let [[options set-options!] (react/useState ::loading)]
    (react/useEffect
     (fn []
       (-> (state/invoke `portal.runtime/get-options)
           (.then set-options!)))
     #js [])
    (into [:r> (.-Provider options-context)
           #js {:value (if (= options ::loading)
                         options
                         (merge options @extension-options))}]
          children)))

(defn use-options [] (react/useContext options-context))
