(ns portal.http-socket-server-test
  (:require [clj-http.lite.client :as client]
            [clojure.test :refer [deftest testing is]]
            [portal.http-socket-server :as server]))

(def resolved (promise))

(defn- handler [request]
  (case ((juxt :method :uri) request)
    ["GET" "/"]
    {:status 200
     :headers {"Content-Type" "text/plain"}
     :body "hello, 🌍"}

    ["GET" "/timeout"]
    (do
      @(:closed? request)
      (deliver resolved ::timeout))

    ["GET" "/error"]
    (throw (ex-info "Error!" {}))

    ["POST" "/"]
    {:status 200 :body (:body request)}

    {:status 404}))

(deftest http-server
  (let [server (server/start handler)
        port   (:port server)
        url    (str "http://localhost:" port)]
    (testing "GET /"
      (is (= {:status 200
              :body "hello, 🌍"
              :headers {"content-length" "11"
                        "content-type" "text/plain"}}
             (client/get url))))
    (testing "GET /not-found"
      (is (= {:status 404 :body "" :headers {}}
             (client/get (str url "/not-found")
                         {:throw-exceptions false}))))
    (testing "GET /error"
      (is (= {:status 500 :body "" :headers {}}
             (client/get (str url "/error")
                         {:throw-exceptions false}))))
    (testing "GET /timeout"
      (is (= ::timeout
             (try
               (client/get (str url "/timeout")
                           {:socket-timeout 10})
               (catch Exception _e @resolved)))))
    (testing "POST /"
      (is (= {:status 200
              :body "echo"
              :headers {"content-length" "4"}}
             (client/post url {:body "echo"}))))
    (server/stop server)))

