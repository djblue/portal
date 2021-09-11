(ns portal.runtime.node.server
  (:require [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.index :as index]
            [portal.runtime.node.client :as c]))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn require-string [src file-name]
  (let [Module (js/require "module")
        m (Module.)]
    (._compile m src file-name)
    (.-exports m)))

(def Server (-> (io/resource "portal/ws.js")
                (require-string "portal/ws.js") .-Server))

(def ops (merge c/ops rt/ops))

(defn- rpc-handler [request _response]
  (let [[_ session-id] (.split (.-url request) "?")
        session        {:session-id (uuid session-id)}]
    (.handleUpgrade
     (Server. #js {:noServer true})
     request
     (.-socket request)
     (.-headers request)
     (fn [ws]
       (let [session (rt/open-session session)
             send!
             (fn send! [message]
               (.send ws (rt/write message session)))]
         (swap! c/connections assoc session-id send!)
         (.on ws "message"
              (fn [message]
                (a/let [req  (rt/read message session)
                        id   (:portal.rpc/id req)
                        op   (get ops (get req :op) not-found)
                        done #(send! (assoc %
                                            :portal.rpc/id id
                                            :op :portal.rpc/response))]
                  (binding [rt/*session* session]
                    (op req done)))))
         (.on ws "close"
              (fn []
                (swap! c/connections dissoc session-id))))))))

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
    (f)))
