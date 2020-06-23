(ns portal.main
  (:require [portal.runtime :as rt]
            [cognitect.transit :as t]
            [portal.async :as a]
            ["http" :as http]
            ["fs" :as fs]
            ["child_process" :as cp]))

;; server.cljs

(defn slurp [file-name]
  (js/Promise.
   (fn [resolve reject]
     (fs/readFile
      file-name
      "utf8"
      (fn [err data]
        (if err (reject err) (resolve data)))))))

(defn get-chrome-bin []
  (js/Promise.resolve "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"))

(defn sh [bin & args]
  (js/Promise.
   (fn [resolve reject]
     (let [ps (cp/spawn bin (clj->js args))]
       (.on ps "error" reject)
       (.on ps "close" resolve)))))

(defonce server (atom nil))

(defn buffer-body [request]
  (js/Promise.
   (fn [resolve reject]
     (let [body (atom "")]
       (.on request "data" #(swap! body str %))
       (.on request "end"  #(resolve @body))
       (.on request "error" reject)))))

(defn instance->uuid [instance]
  (let [k [:instance instance]]
    (-> rt/instance-cache
        (swap!
         (fn [cache]
           (if (contains? cache k)
             cache
             (let [uuid (random-uuid)]
               (assoc cache [:uuid uuid] instance k uuid)))))
        (get k))))

(defn uuid->instance [uuid]
  (get @rt/instance-cache [:uuid uuid]))

(defn find-var [s] (get @rt/instance-cache [:var s]))

(defn json->edn [json]
  (t/read
   (t/reader
    :json
    {:handlers
     {"portal.transit/var"
      (t/read-handler
       (fn [s] (let [[_ pair] s] (find-var (first pair)))))
      "portal.transit/object" (t/read-handler (comp uuid->instance :id))}})
   json))

(defn var->symbol [v]
  (let [m (meta v)]
    (symbol (str (:ns m)) (str (:name m)))))

;; Using the normal t/write-meta causes an issue with vars. Since
;; transit-cljs seems to process metadata before custom handlers, the
;; metadata is stripped from the var, rendering the handler useless.
;; Moreover, metadata is then associated with the tagged value, resultsing
;; in issues for consumers because Transit$TaggedValue doesn't implement
;; any metadata protocols. I don't know if this is inteded behavior or a
;; bug, it seems to work without issue for transit-clj.

(defn write-meta
  "For :transform. Will write any metadata present on the value.
  Forked from https://github.com/cognitect/transit-cljs/blob/master/src/cognitect/transit.cljs#L412"
  [x]
  (cond
    (instance? cljs.core/Var x)
    (let [s (var->symbol x)]
      (swap! rt/instance-cache assoc [:var s] x)
      (t/tagged-value "portal.transit/var" (with-meta s (meta x))))

    (implements? IMeta x)
    (if-let [m (-meta ^not-native x)]
      (t/WithMeta. (-with-meta ^not-native x nil) m)
      x)

    :else x))

(defn edn->json [edn]
  (t/write
   (t/writer
    :json
    {:transform write-meta
     :handlers
     {js/URL
      (t/write-handler (constantly "r") str)
      :default
      (t/write-handler
       (constantly "portal.transit/object")
       (fn [o]
         {:id (instance->uuid o) :type (pr-str (type o)) :string (pr-str o)}))}})
   edn))

(defn not-found [_request done]
  (done {:status :not-found}))

(defn send-rpc [response value]
  (-> response
      (.writeHead 200 #js {"Content-Type"
                           "application/transit+json; charset=utf-8"})
      (.end (try
              (edn->json
               (assoc value :portal.rpc/exception nil))
              (catch js/Error e
                (edn->json
                 {:portal/state-id (:portal/state-id value)
                  :portal.rpc/exception e}))))))

(defn rpc-handler [request response]
  (a/let [body    (buffer-body request)
          req     (json->edn body)
          op      (get rt/ops (get req :op) not-found)
          done    #(send-rpc response %)
          cleanup (op req done)]
    (when (fn? cleanup)
      (.on request "close" cleanup))))

(defn send-resource [response content-type resource-name]
  (a/let [body (slurp (str "resources/" resource-name))]
    (-> response
        (.writeHead 200 #js {"Content-Type" content-type})
        (.end body))))

(defn handler [request response]
  (let [paths
        {"/"        #(send-resource response "text/html"       "index.html")
         "/main.js" #(send-resource response "text/javascript" "main.js")
         "/rpc"     #(rpc-handler request response)}

        f (get paths (.-url request) #(-> response (.writeHead 404) .end))]
    (when (fn? f) (f))))

(defn start [handler]
  (js/Promise.
   (fn [resolve reject]
     (let [server (http/createServer #(handler %1 %2))]
       (.listen server 0
                #(let [port (.-port (.address server))
                       result (with-meta {:server server} {:local-port port})]
                   (resolve result)))))))

(defn stop [handle]
  (.close (:server handle)))

(defn open-inspector [value]
  (swap! rt/state assoc :portal/open? true)
  (rt/update-value value)
  (a/let [chrome-bin (get-chrome-bin)
          instance   (or @server (start #'handler))]
    (reset! server instance)
    (sh chrome-bin
        "--incognito"
        "--disable-features=TranslateUI"
        "--no-first-run"
        (str "--app=http://localhost:" (-> instance meta :local-port)))))

(defn close-inspector []
  (swap! rt/state assoc :portal/open? false)
  (stop @server)
  (reset! server nil))

(defn -main [])

(comment
  (add-tap #'rt/update-value)

  (tap> #'rt/update-value)

  (require 'examples.hacker-news)
  (tap> examples.hacker-news/stories)

  (tap> (slurp "deps.edn"))
  (tap> (js/Promise.resolve 1))
  (tap> 1)

  (extend-protocol clojure.core.protocols/Datafiable
    js/Promise
    (datafy [this] (.then this identity)))

  (open-inspector 1)
  (-> @rt/instance-cache)
  (-> @server)
  (close-inspector)
  (rt/clear-values)

  (stop @server))
