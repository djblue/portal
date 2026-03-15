(ns portal.ssr.ui.core
  (:require ["idiomorph" :as i]
            [portal.async :as a]
            [portal.ui.rpc :as rpc]))

(defn render [html]
  (i/morph (.getElementById js/document "root")
           html
           #js {:morphStyle "innerHTML"}))

(defn- parent-elements [el]
  (take-while some? (iterate (fn [^js el] (.-parentElement el)) el)))

(defn find-on-click [el]
  (some
   (fn [^js el]
     (.getAttribute el "data-on-click"))
   (parent-elements el)))

(defn main! []
  (render "<h1>hello, worlds</h1>")
  (a/let [ws (rpc/connect {:path "/ssr"
                           :on-message
                           (fn [data]
                             (render data))})]
    (.addEventListener
     js/window
     "keydown"
     (fn [^js e]
       (.send ^js ws
              (.stringify
               js/JSON
               #js
                {:op "on-key-down"
                 :key (.toLowerCase (.-key e))
                 :ctrl-key (.-ctrlKey e)
                 :meta-key (.-metaKey e)
                 :shift-key (.-shiftKey e)
                 :alt-key (.-altKey e)}))))
    (.addEventListener
     (.getElementById js/document "root")
     "click"
     (fn [^js e]
       (when-let [id (find-on-click (.-srcElement e))]
         (.send ^js ws
                (.stringify
                 js/JSON
                 #js {:op "on-click"
                      :id id
                      :ctrl-key (.-ctrlKey e)
                      :meta-key (.-metaKey e)
                      :shift-key (.-shiftKey e)
                      :alt-key (.-altKey e)})))))))