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
        ^js m (Module. file-name js/module.parent)]
    (set! (.-filename m) file-name)
    (._compile m src file-name)
    (.-exports m)))

(def Server (-> (io/resource "portal/ws.js")
                (require-string "portal/ws.js") .-Server))

(def ops (merge c/ops rt/ops))

(defn- get-session-id [^js req]
  (some->
   (or (last (str/split (.-url req) #"\?"))
       (when-let [referer (.getHeader req "referer")]
         (last (str/split referer #"\?"))))
   uuid))

(defn- get-session [req]
  (rt/get-session (get-session-id req)))

(defn- rpc-handler [^js req _res]
  (let [session (get-session req)]
    (.handleUpgrade
     (Server. #js {:noServer true})
     req
     (.-socket req)
     (.-headers req)
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

(defn- send-resource [^js res content-type body]
  (-> res
      (.writeHead 200 #js {"Content-Type" content-type})
      (.end body)))

(defn- main-js [req]
  (let [options (-> req get-session :options)]
    (case (:mode options)
      :dev (fs/slurp (get-in options [:resource "main.js"]))
      (io/resource "portal/main.js"))))

(defn- get-path [^js req _res]
  (-> req .-url (str/split #"\?") first))

(defmulti handler #'get-path)

(defmethod handler "/" [_req res]
  (send-resource res "text/html" (index/html)))

(defmethod handler "/main.js" [req res]
  (send-resource res "text/javascript" (main-js req)))

(defmethod handler "/rpc" [req res] (rpc-handler req res))

(defmethod handler :default [_req ^js res] (-> res (.writeHead 404) .end))
