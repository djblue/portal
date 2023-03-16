(ns examples.fetch)

(defn- node-fetch [url]
  (let [http (js/require "https")]
    (js/Promise.
     (fn [resolve reject]
       (.end
        (.request
         http
         url
         (fn [res]
           (let [body (atom "")]
             (.on res "data" #(swap! body str %))
             (.on res "error" reject)
             (.on res "end" #(resolve @body))))))))))

(defn- web-fetch [url]
  (.then (js/fetch url) #(.text %)))

(def fetch (if (some? js/window) web-fetch node-fetch))
