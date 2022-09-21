# Portal Standalone

<p align="center">
  <a href="https://djblue.github.io/portal/">
  <img width="640"
     alt="Portal Standalone"
     src="https://user-images.githubusercontent.com/1986211/191418745-ce7cf62b-cc19-4c32-a573-02b3f3fc35e7.png" />
  </a>
</p>

To use Portal without any particular runtime, try the
[standalone](https://djblue.github.io/portal/) version of Portal. It can be
installed as a [chrome pwa](https://support.google.com/chrome/answer/9658361)
which will provide a dock launcher for easy access.

![install](https://user-images.githubusercontent.com/1986211/191423393-28b65580-fa51-439b-bf16-1c4f40953cd8.png)

## Preloading Data

To open the UI with pre-loaded data, you can pass the `content-url` and
`content-type` via query params. For example
[this link](https://djblue.github.io/portal/?content-url=https://gist.githubusercontent.com/djblue/9a2cd250e061f62ce527b20648fd8256/raw/e7bd673df60b3c503306956b950bb9589ba480eb/data.clj&content-type=application/edn) pulls some edn data via a [GitHub Gist](https://gist.github.com/djblue/6ef5f3ddd0bc93e3ca34a7132be63d8f) with the `content-type=application/edn`, this can be very handy to share data.

The supported content-types:

- `application/edn`
- `application/json`
- `application/transit+json`
- `text/plain`

## Share Command

If you want to automate the sharing of data from your runtime, you can create a
[Portal command](../ui/commands.md) that automatically uploads and generates a
similar link as above. One such command could look like:

```clojure
(ns portal.share
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [org.httpkit.client :as http]
            [portal.api :as p]
            [portal.runtime.json :as json]))
​
(defn- pprint [value]
  (binding [*print-meta* true]
    (with-out-str (pp/pprint value))))
​
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
​
(defn- ->query-string [m]
  (str/join "&" (for [[k v] m] (str (name k) "=" v))))
​
(defn- create-url [gist-raw-url]
  (str "https://djblue.github.io/portal/?"
       (->query-string {:content-url gist-raw-url
                        :content-type "application/edn"})))
​
(defn- shorten-url [url]
  (-> @(http/post
        "https://url.api.stdlib.com/temporary@0.3.0/create/"
        {:headers {"Content-Type" "application/json"}
         :body    (json/write {:url url :ttl 86400})})
      :body
      json/read
      :link_url))
​
(defn share
  "Create a shareable portal link for the supplied value."
  [value]
  (-> value pprint create-gist create-url shorten-url))
​
(p/register! #'share)
```
