(ns ^:no-doc portal.runtime.node.server
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [goog.object :as g]
            [portal.async :as a]
            [portal.resources :as io]
            [portal.runtime :as rt]
            [portal.runtime.fs :as fs]
            [portal.runtime.index :as index]
            [portal.runtime.json :as json]
            [portal.runtime.node.client :as c]))

(defn- get-header [^js req k]
  (-> req .-headers (g/get k)))

(defn- not-found [_request done]
  (done {:status :not-found}))

(defn- require-string [src file-name]
  (let [Module (js/require "module")
        ^js m (Module. file-name js/module.parent)]
    (set! (.-filename m) file-name)
    (._compile m src file-name)
    (.-exports m)))

(def Server (-> (io/inline "portal/ws.js")
                (require-string "portal/ws.js") .-Server))

(def ops (merge c/ops rt/ops))

(defn- get-session-id [^js req]
  (some->
   (or (last (str/split (.-url req) #"\?"))
       (when-let [referer (get-header req "referer")]
         (last (str/split referer #"\?"))))
   uuid))

(defn- get-session [req]
  (some-> req get-session-id rt/get-session))

(defn- get-path [^js req _res]
  [(-> req .-method str/lower-case keyword)
   (-> req .-url (str/split #"\?") first)])

(defmulti route #'get-path)

(defmethod route [:get "/rpc"] [^js req _res]
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
         (when-let [f (get-in session [:options :on-load])]
           (f))
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

(defmethod route [:get "/"] [req ^js res]
  (if-let [session (get-session req)]
    (send-resource res "text/html" (index/html (:options session)))
    (let [session-id (random-uuid)]
      (swap! rt/sessions assoc session-id {})
      (doto res
        (.writeHead
         307
         #js {"Location" (str "?" session-id)})
        (.end)))))

(defmethod route [:get "/icon.svg"] [_req res]
  (send-resource
   res
   "image/svg+xml"
   (io/inline "portal/icon.svg")))

(defmethod route [:get "/main.js"] [req res]
  (let [options (-> req get-session :options)]
    (send-resource
     res
     "text/javascript"
     (case (:mode options)
       :dev (fs/slurp (get-in options [:resource "main.js"]))
       (io/inline "portal/main.js")))))

(defn get-body [^js req]
  (js/Promise.
   (fn [resolve reject]
     (let [body (atom "")]
       (.on req "data" #(swap! body str %))
       (.on req "end"  #(resolve @body))
       (.on req "error" reject)))))

(defmethod route [:post "/submit"] [^js req ^js res]
  (a/let [body (get-body req)]
    (rt/update-value
     (case (get-header req "content-type")
       "application/transit+json" (transit/read (transit/reader :json) body)
       "application/json"         (js->clj (json/read body))
       "application/edn"          (edn/read-string {:default tagged-literal} body)))
    (doto res
      (.writeHead
       204
       #js {"Access-Control-Allow-Origin" "*"})
      (.end))))

(defmethod route [:options "/submit"] [_req ^js res]
  (doto res
    (.writeHead
     204
     #js {"Access-Control-Allow-Origin"  "*"
          "Access-Control-Allow-Headers" "origin, content-type"
          "Access-Control-Allow-Methods" "POST, GET, OPTIONS, DELETE"
          "Access-Control-Max-Age"       86400})
    (.end)))

(defmethod route :default [_req ^js res] (-> res (.writeHead 404) .end))

(defn handler [req res] (route req res))
