(ns portal.runtime.remote.server
  (:require [clojure.core.server :as server]
            [portal.runtime.jvm.client :as c]
            [portal.runtime :as rt]
            [clojure.edn :as edn]))

(defonce ^:private value-cache (atom {}))

(def options
  {:parse       edn/read-string
   :stringify   pr-str
   :value-cache value-cache})

(defn- not-found [_request done]
  (done {:status :not-found}))

(def ^:private ops (merge rt/ops))

(defn- session-value-cache [value-cache]
  (let [session server/*session*]
    (if (= (meta value-cache) session)
      value-cache
      (with-meta {} session))))

(defn- setup-client! [out]
  (swap! c/sessions
         assoc
         ::remote
         (fn send! [message]
           (binding [*out* out]
             (println
              (rt/write message options))))))

(defn request [message]
  (swap! (:value-cache options) session-value-cache)
  (setup-client! *out*)
  (let [body    (rt/read message options)
        id      (:portal.rpc/id body)
        out     *out*
        op      (get ops (:op body) not-found)]
    (future
      (binding [rt/*options* options]
        (op body #(binding [*out* out]
                    (println
                     (rt/write
                      (assoc %
                             :portal.rpc/id id
                             :op :portal.rpc/response)
                      options)))))))
  nil)

(add-tap #'rt/update-value)
