(ns portal.runtime.node.server
  (:require [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.node.client :as c]))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- edn->json [value]
  (try
    (rt/write
     (assoc value :portal.rpc/exception nil))
    (catch js/Error e
      (rt/write
       {:portal/state-id (:portal/state-id value)
        :portal.rpc/exception e}))))

(defn require-string [src file-name]
  (let [Module (js/require "module")
        m (Module.)]
    (._compile m src file-name)
    (.-exports m)))

(def Server (-> (io/resource "portal/ws.js")
                (require-string "portal/ws.js") .-Server))

(def ops (merge c/ops rt/ops))

(defn- rpc-handler [request _response]
  (let [[_ session-id] (.split (.-url request) "?")]
    (.handleUpgrade
     (Server. #js {:noServer true})
     request
     (.-socket request)
     (.-headers request)
     (fn [ws]
       (let [send!
             (fn send! [message]
               (.send ws (edn->json message)))]
         (swap! c/sessions assoc session-id send!)
         (.on ws "message"
              (fn [message]
                (a/let [req  (rt/read message)
                        id   (:portal.rpc/id req)
                        op   (get ops (get req :op) not-found)
                        done #(send! (assoc %
                                            :portal.rpc/id id
                                            :op :portal.rpc/response))]
                  (op req done))))
         (.on ws "close"
              (fn []
                (swap! c/sessions dissoc session-id))))))))

(defn- send-resource [response content-type body]
  (-> response
      (.writeHead 200 #js {"Content-Type" content-type})
      (.end body)))

(defn handler [request response]
  (let [paths
        {"/"        #(send-resource response "text/html"       (index/html))
         "/main.js" #(send-resource response "text/javascript" (io/resource "portal/main.js"))
         "/rpc"     #(rpc-handler request response)}
        [path] (.split (.-url request) "?")
        f (get paths path #(-> response (.writeHead 404) .end))]
    (when (fn? f) (f))))
