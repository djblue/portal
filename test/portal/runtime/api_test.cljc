(ns portal.runtime.api-test
  (:require [clojure.test :refer [deftest is]]
            [portal.api :as p]
            [portal.runtime.browser :as-alias browser]))

(defn- headless-chrome-flags [url]
  ["--headless=new" "--disable-gpu" "--no-sandbox" url])

(defn- open []
  (p/open {:mode :test
           ::browser/chrome-bin ["chromium"]
           ::browser/flags headless-chrome-flags}))

(defn- test-atom [portal]
  (reset! portal 0)
  (is (= @portal 0))
  (swap! portal inc)
  (is (= @portal 1)))

(deftest e2e-jvm-test
  (let [portal (open)]
    #?(:lpy :skip
       :default (test-atom portal))
    (is (= 6 (p/eval-str portal "(+ 1 2 3)")))
    (is (= 6 (p/eval-str portal "*1")))
    (is (= :world (:hello (p/eval-str portal "{:hello :world}"))))
    (is (thrown?
         #?(:lpy Exception :cljs :default :default clojure.lang.ExceptionInfo)
         (p/eval-str portal "(throw (ex-info \"error\" {:hello :world}))")))
    (is (= :hi (p/eval-str portal "(.resolve js/Promise :hi)" {:await true})))
    (is (some? (some #{portal} (p/sessions))))
    (p/close portal)))
