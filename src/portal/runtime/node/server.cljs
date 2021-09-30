(ns portal.runtime.node.server
  (:require [clojure.string :as str]
            [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.node.client :as c]))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- require-string [src file-name]
  (let [Module (js/require "module")
        m (Module. file-name js/module.parent)]
    (set! (.-filename m) file-name)
    (._compile m src file-name)
    (.-exports m)))

(def Server (-> (io/resource "portal/ws.js")
                (require-string "portal/ws.js") .-Server))

(def ops (merge c/ops rt/ops))

(defn- get-session-id [request]
  (some->
   (or (last (.split (.-url request) "?"))
       (when-let [referer (.getHeader request "referer")]
         (last (str/split referer #"\?"))))
   uuid))

(defn- get-session [request]
  (rt/get-session (get-session-id request)))

(defn- rpc-handler [request _response]
  (let [session (get-session request)]
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
         (swap! c/connections assoc (:session-id session) send!)
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
                (swap! c/connections dissoc (:session-id session)))))))))

(defn- send-resource [response content-type body]
  (-> response
      (.writeHead 200 #js {"Content-Type" content-type})
      (.end body)))

(defn- main-js [request]
  (let [options (-> request get-session :options)]
    (case (:mode options)
      :dev (fs/slurp (get-in options [:resource "main.js"]))
      (io/resource "portal/main.js"))))

(defn handler [request response]
  (let [paths
        {"/"        #(send-resource response "text/html"       (index/html))
         "/main.js" #(send-resource response "text/javascript" (main-js %))
         "/rpc"     #(rpc-handler request response)}
        [path] (.split (.-url request) "?")
        f (get paths path #(-> response (.writeHead 404) .end))]
    (f request)))
