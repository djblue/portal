(ns portal.jvm-test
  (:require [clojure.test :refer [deftest is]]
            [portal.api :as p]
            [portal.runtime.browser :as browser]))

(defn- headless-chrome-flags [url]
  ["--headless" "--disable-gpu" url])

(defn- open [f]
  (with-redefs [browser/flags f] (p/open {:mode :test})))

(deftest e2e-jvm
  (when-let [portal (open headless-chrome-flags)]
    (reset! portal 0)
    (is (= @portal 0))
    (swap! portal inc)
    (is (= @portal 1))
    (is (= 6 (p/eval-str portal "(+ 1 2 3)")))
    (is (= 6 (p/eval-str portal "*1")))
    (is (= :world (:hello (p/eval-str portal "{:hello :world}"))))
    (is (thrown?
         clojure.lang.ExceptionInfo
         (p/eval-str portal "(throw (ex-info \"error\" {:hello :world}))")))
    (is (= :hi (p/eval-str portal "(.resolve js/Promise :hi)" {:await true})))
    (is (some? (some #{portal} (p/sessions))))
    (p/close portal)))
