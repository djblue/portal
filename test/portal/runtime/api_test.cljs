(ns portal.runtime.api-test
  (:require [clojure.test :refer [async deftest is]]
            [portal.api :as p]
            [portal.async :as a]
            [portal.runtime.browser :as browser]))

(defn- headless-chrome-flags [url]
  ["--headless=new" "--disable-gpu" "--no-sandbox" url])

(defn- open []
  (p/open {:mode :test
           ::browser/chrome-bin ["chromium"]
           ::browser/flags headless-chrome-flags}))

(defn- is= [a b]
  (a/let [a' a b' b] (is (= a' b'))))

(deftest e2e-node-test
  (async done
         (a/let [portal (open)]
           (reset! portal 0)
           (is (= @portal 0))
           (swap! portal inc)
           (is (= @portal 1))
           (is= 6 (p/eval-str portal "(+ 1 2 3)"))
           (is= 6 (p/eval-str portal "*1"))
           (is= {:hello :world} (p/eval-str portal "{:hello :world}"))
           (-> (p/eval-str portal "(throw (ex-info \"error\" {:hello :world}))")
               (.then (fn [] (throw (ex-info "Should throw error" {}))))
               (.catch (fn [_])))
           (is= :hi (p/eval-str portal "(.resolve js/Promise :hi)" {:await true}))
           (is (some? (some #{portal} (p/sessions))))
           (p/close portal)
           (done))))
