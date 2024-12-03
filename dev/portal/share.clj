(ns portal.share
  (:require
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [org.httpkit.client :as http]
   [portal.api :as p]
   [portal.runtime.json :as json])
  (:import
   (java.net URL)))

(defn- pprint [value]
  (binding [*print-meta* true]
    (with-out-str (pp/pprint value))))

(defn- create-gist [content]
  (-> @(http/post
         "https://api.github.com/gists"
         {:basic-auth [(System/getenv "GIT_USERNAME")
                       (System/getenv "GIT_PASSWORD")]
          :headers    {"Accept" "application/vnd.github.v3+json"}
          :body       (json/write
                        {:public      false
                         :description "Portal Share"
                         :files       {"data.clj" {:content content}}})})
      :body
      json/read
      (get-in [:files :data.clj :raw_url])))

(defn- ->query-string [m]
  (str/join "&" (for [[k v] m] (str (name k) "=" v))))

(defn- create-url [gist-raw-url]
  (str "https://djblue.github.io/portal/?"
       (->query-string {:content-url gist-raw-url
                        :content-type "application/edn"})))

(defn- shorten-url [url]
  (-> @(http/post
         "https://url.api.stdlib.com/temporary@0.3.0/create/"
         {:headers {"Content-Type" "application/json"}
          :body    (json/write {:url url :ttl 86400})})
      :body
      json/read
      :link_url))

(defn share
  "Create a shareable portal link for the supplied value."
  [value]
  (URL. (-> value pprint create-gist create-url shorten-url)))

(p/register! #'share)
