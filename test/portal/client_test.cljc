(ns portal.client-test
  #?(:clj
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.jvm :as c]
               [portal.runtime :as rt]
               [portal.sync :as a])
     :cljr
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.client.clr :as c]
               [portal.runtime :as rt]
               [portal.sync :as a])
     :cljs
     (:require [clojure.test :refer [async deftest is]]
               [portal.api :as p]
               [portal.async :as a]
               [portal.client.node :as c]
               [portal.runtime :as rt])

     :lpy
     (:require [clojure.test :refer [deftest is]]
               [portal.api :as p]
               [portal.sync :as a]
               [portal.client.py :as c]
               [portal.runtime :as rt])))

(def ^:private bad-seq (map (fn [_] (throw (ex-info "Error" {}))) (range 10)))

(defn- client-test* [done]
  (a/let [opts {:port 12345 :host "127.0.0.1"}
          tap-list @#'rt/tap-list]
    (swap! tap-list empty)
    (p/start opts)
    (c/submit opts ::value)
    (c/submit opts bad-seq)
    (is (= "Error" (:cause (first @tap-list))))
    (is (= ::value (second @tap-list)))
    (done)))

(deftest client-test
  #?(:cljs    (async done (client-test* done))
     :default (client-test* (constantly nil))))