(ns portal.ui.core
  (:require ["react" :as react]
            [clojure.string :as str]
            [portal.ui.app :as app]
            [portal.ui.connecton-status :as conn]
            [portal.ui.options :as opts]
            [portal.ui.rpc :as rpc]
            [portal.ui.sci :as sci]
            [portal.ui.sci.libs :as libs]
            [portal.ui.state :as state]
            [reagent.core :as r]
            [reagent.dom :as dom]))

(def functional-compiler (r/create-compiler {:function-components true}))

(defn use-tap-list []
  (let [a (rpc/use-invoke 'portal.runtime/get-tap-atom)]
    (rpc/use-invoke 'clojure.core/deref a)))

(defn- default-app [] [app/app (use-tap-list)])

(defn- ns->url [ns]
  (str "/"
       (str/replace (name ns) #"\." "/")
       ".cljs"))

(defn- custom-app [opts]
  (let [[app set-app!] (react/useState nil)]
    (react/useEffect
     (fn []
       (-> (js/fetch (ns->url (:main opts)))
           (.then #(.text %))
           (.then sci/eval-string)
           (.then set-app!)))
     #js [])
    (when app [app/root [app]])))

(defn connected-app []
  (let [opts (opts/use-options)]
    [conn/with-status
     (cond
       (= opts ::opts/loading) nil
       (contains? opts :main) [custom-app opts]
       :else [default-app])]))

(defn with-cache [& children]
  (into [:<> (meta @state/value-cache)] children))

(defn render-app []
  (dom/render [with-cache
               [opts/with-options
                [connected-app]]]
              (.getElementById js/document "root")
              functional-compiler))

(defn- load-fn [{:keys [namespace]}]
  (let [file (ns->url namespace)
        xhr  (js/XMLHttpRequest.)]
    (.open xhr "GET" file false)
    (.send xhr nil)
    {:file   file
     :source (.-responseText xhr)}))

(defn main! []
  (reset! sci/ctx (libs/init {:load-fn load-fn}))
  (reset! state/sender rpc/request)
  (render-app))

(defn reload! [] (render-app))
